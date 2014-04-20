package edu.msu.mi.teva.wsj

import com.csvreader.CsvReader
import edu.mit.cci.adapters.csv.CsvBasedConversation
import edu.mit.cci.teva.MemoryBasedRunner
import edu.mit.cci.teva.engine.CommunityModel
import edu.mit.cci.teva.engine.TevaParameters
import edu.mit.cci.teva.model.Conversation
import edu.mit.cci.teva.util.TevaUtils
import groovy.io.FileType
import edu.mit.cci.util.U

import javax.swing.JFileChooser


/**
 * Created by charlesbean on 4/18/14.
 */
class WSJImplementation {

    /*******************************************************/
    //Setting a couple of different parameters (based off of ICMI currently)
    def params = [[window: 50, delta: 10], [window: 100, delta: 10], [window: 100, delta: 20], [window: 120, delta:30]]

    //Regex expression for file prefixes (1.exWSJ.ref/1.exWSJ.csv)
    def fileRegex = ~/(.*)(?:.csv|.ref)/  //Full name at [0][0]; Name without extension at [0][1]

    //For systematic checking
    def passed(int count = 0){
        printf("--PASSED %d REF AND CSV FILES--\n", count)
    }
    /*******************************************************/



    /**
     * Implementing the WSJ module over a directory of corpora ("Data") - it
     should consist of one folder of .csv files ("Extracted"), and
     one folder of .ref files ("Ref Files")
     *
     * @param dir
     */
    WSJImplementation(File dir){

        //Creating a map of processes/count for total files
        def process_map = [:]
        int count = 0

        /* Iterate through subdirectories, then files. Creates
                a map that looks like [FILE: [data (.csv) file, reference (.ref) file], FILE2: [data, ref], ...etc]
         */
        dir.eachFile { subdir ->

            //Two subdirectories of .csv or .ref files (default from "WSJtoCSV.py")
            if (subdir.isDirectory()){
                subdir.eachFileMatch(FileType.FILES, ~/.*(?:csv|ref)/) { f -> //Each FILE that ends in .csv or .ref
                    def prefix = (f.getName() =~ fileRegex)[0][1]

                    if (!process_map[prefix]) { //If prefix isnt within map, add key (value a map of data and ref)
                        process_map[prefix] = [:]
                    }

                    if (f.getName().endsWith("ref")) { //If the file extension is .ref, add to submap [1]
                        process_map[prefix]["ref"] = f

                    }

                    else {
                        process_map[prefix]["data"] = f //If .csv, add to submap [0]
                    }

                    count += 1
                }
            }
        }
        process_map.keySet().retainAll(process_map.findResults { (it.value["ref"] && it.value["data"]) ? it.key : null })

        passed(count)

        run(process_map)
    }

    /**
     * Run TEvA over a map
     *
     * @param data
     * @return
     */
    def run(Map data){

        File out = new File("WSJOutput.csv") //Final output file

        out.withWriterAppend {
            it.println("corpus, window, delta, pmiss, pfa, pk") //Write columns
        }

        data.each { String k, v -> //For each file prefix (k), and each ref/csv file (v[csv] or v[ref])
            runInstance(k, v["data"] as File, v["ref"] as File, out)
        }
    }

    /**
     * Run instance of TEvA on a member of process_map
     *
     * @param name
     * @param data
     * @param ref
     * @param out
     */
    def runInstance(String name, File data, File ref, File out){

        //Creating conversation (per file)
        Conversation conv = initConversation(data)

        //Creating segmentation data
        List<Integer> segs = segmentationData(ref, conv)

        //Loading parameters
        TevaParameters tevaParams = new TevaParameters(System.getResourceAsStream("/wsj.teva.properties"))


    }

    /**
     * Initializes a Conversation based on the data file
     *
     * @param data
     * @return
     */
    def initConversation(File data){

        //Parameters are (columns, filename, filestream, delimiter (tab), textQual)
        return new CsvBasedConversation(["id", "replyTo", "start", "author", "text"] as String[], data.getName(), data.newInputStream(), '\t' as char, false) {
            public Date processDate(CsvBasedConversation.Column field, CsvReader reader) {
                new Date(((processString(field, reader) as float) * 1000f) as long)
            }
        }
    }

    /**
     * Creates segmentation data -> pk
     *
     * @param ref
     * @param c
     * @return
     */
    def segmentationData(File ref, Conversation conv){

        //array of segmentation data
        def segdata = []

        //List of integers of same data
        List<Integer> refNumbers = []

        //Add to array from reference file
        ref.readLines().each { line ->

            //Each line should be a 1 or 0 - this catches all digits
            def seg = line =~ /([\d]+)/ //Matcher instance

            //If match is 1 or more chars, add the 1st char as long
            if (seg.size() > 0) {
                segdata << Long.parseLong((seg[0][0] as String).replaceAll(/\./,""))
            }
        }

        //Iterate threads
        conv.allThreads.each { thread ->

            //Posts
            def sidx = 0

            thread.posts.each { post ->

                if (sidx < segdata.size() && post.time.time >= segdata[sidx]) {
                    sidx++
                    refNumbers << 1
                }

                else {
                    refNumbers << 0
                }
            }
        }

        refNumbers

    }

    /**
     * Calculates the pK value of two lists (commonality) [COPIED]
     *
     * @param ref
     * @param hyp
     * @param boundary
     * @return
     */
    def static Map pk(List<Integer> ref, List<Integer> hyp, boundary = 1) {
        def k = Math.round(ref.size() / (ref.count(boundary) * 2)) as int
        println "K is " + k
        def nConsidered = ref.size() - k - 1
        def nSameRef = 0f
        def nFalseAlarm = 0f
        def nMiss = 0

        (0..nConsidered).each { i ->
            def bSameRefSeg = false
            def bSameHypSeg = false

            if (!(boundary in ref[(i + 1)..(i + k)])) {
                nSameRef += 1
                bSameRefSeg = true

            }
            if (!(boundary in hyp[(i + 1)..(i + k)])) {
                bSameHypSeg = true
            }

            if (!bSameRefSeg && bSameHypSeg) {
                nMiss += 1
            }
            if (bSameRefSeg && !bSameHypSeg) {
                nFalseAlarm += 1
            }
        }

        def probSameRef = nSameRef / nConsidered
        def probDiffRef = 1 - probSameRef
        def probMiss = nMiss / nConsidered
        def probFalseAlarm = nFalseAlarm / nConsidered


        return [pMiss: probMiss, pFalseAlarm: probFalseAlarm, pk: probMiss * probDiffRef + probFalseAlarm * probSameRef]
    }

    static void main(String[] args){
        //File f = U.getAnyFile("WSJ Directory", ".", JFileChooser.DIRECTORIES_ONLY)
        File WSJdir = new File("/Users/charlesbean/Code/TEvA/Corpora/Converted/WSJ/Data")
        if (!WSJdir.isDirectory()){
            println "Whoops! Not a directory"
        }
        else{
            new WSJImplementation(WSJdir)
        }
    }
}
