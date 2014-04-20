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
        printf("--PASSED %d FILES--\n", count)
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

        Conversation c = initConversation(data)

        List<Integer> segs = segmentationData(ref, c)

    }

    /**
     * Initializes a Conversation based on the data file
     *
     * @param data
     * @return
     */
    def initConversation(File data){

        //Parameters are (columns, filename, filestream, delimiter (tab), textQual)
        return new CsvBasedConversation(["id", "replyTo", "start", "author", "text"] as String[], data.getName(), data.newInputStream(), 't' as char, false) {
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
    def segmentationData(File ref, Conversation c){
        def segdata = [] //array of segmentation data
        ref.readLines().each { line ->
            def m = line =~ /([\d\.]+)/
            print m
        }

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
