package edu.msu.mi.teva.wsj

import com.csvreader.CsvReader
import edu.mit.cci.adapters.csv.CsvBasedConversation
import edu.mit.cci.teva.MemoryBasedRunner
import edu.mit.cci.teva.engine.Community
import edu.mit.cci.teva.engine.CommunityModel
import edu.mit.cci.teva.engine.ConversationChunk
import edu.mit.cci.teva.engine.TevaParameters
import edu.mit.cci.teva.model.Conversation
import edu.mit.cci.teva.model.Post
import edu.mit.cci.teva.util.TevaUtils
import edu.mit.cci.text.windowing.Windowable
import groovy.io.FileType
import edu.mit.cci.util.U
import groovy.util.logging.Log4j
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator

import javax.swing.JFileChooser


/**
 * Created by charlesbean on 4/18/14.
 */
@Log4j
class WSJImplementation {

    /** *****************************************************/
    //Setting a couple of different parameters (based off of ICMI currently)
    def params = [[fixed_clique_size: 3, minimum_link_weight: 0.85],
            [fixed_clique_size: 3, minimum_link_weight: 0.9],
            [fixed_clique_size: 4, minimum_link_weight: 0.85],
            [fixed_clique_size: 4, minimum_link_weight: 0.9],
            [fixed_clique_size: 5, minimum_link_weight: 0.85],
            [fixed_clique_size: 5, minimum_link_weight: 0.9],
            [fixed_clique_size: 6, minimum_link_weight: 0.85],
            [fixed_clique_size: 6, minimum_link_weight: 0.9]]

    def headers = params.collect() { m ->
        m.keySet()
    }.flatten() as Set

    //Regex expression for file prefixes (1.exWSJ.ref/1.exWSJ.csv)
    def fileRegex = ~/(.*)(?:.csv|.ref)/  //Full name at [0][0]; Name without extension at [0][1]

    //For systematic checking
    def passed(int count = 0) {
        printf("--PASSED %d REF AND CSV FILES--\n", count)
    }
    /** *****************************************************/

    /**
     * Implementing the WSJ module over a directory of corpora ("Data") - it
     should consist of one folder of .csv files ("Extracted"), and
     one folder of .ref files ("Ref Files")
     *
     * @param dir
     */
    WSJImplementation(File dir) {

        //Creating a map of processes/count for total files
        def process_map = [:]
        int count = 0

        /* Iterate through subdirectories, then files. Creates
                a map that looks like [FILE: [data (.csv) file, reference (.ref) file], FILE2: [data, ref], ...etc]
         */
        dir.eachFile { subdir ->

            //Two subdirectories of .csv or .ref files (default from "WSJtoCSV.py")
            if (subdir.isDirectory()) {
                subdir.eachFileMatch(FileType.FILES, ~/.*(?:csv|ref)/) { f -> //Each FILE that ends in .csv or .ref
                    def prefix = (f.getName() =~ fileRegex)[0][1]

                    if (!process_map[prefix]) { //If prefix isnt within map, add key (value a map of data and ref)
                        process_map[prefix] = [:]
                    }

                    if (f.getName().endsWith("ref")) { //If the file extension is .ref, add to submap [1]
                        process_map[prefix]["ref"] = f

                    } else {
                        process_map[prefix]["data"] = f //If .csv, add to submap [0]
                    }

                    count += 1
                }
            }
        }
        process_map.keySet().retainAll(process_map.findResults {
            (it.value["ref"] && it.value["data"]) ? it.key : null
        })

        passed(count)

        run(process_map)
    }

    /**
     * Run TEvA over a map
     *
     * @param data
     * @return
     */
    def run(Map data) {

        File out = new File("WSJOutput.csv") //Final output file

        out.withWriterAppend {
            it.println "corpus,${headers.join(",")},pmiss,pfa,pk"


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
    def runInstance(String name, File data, File ref, File out) {

        //Creating conversation (per file)
        Conversation conv = initConversation(data)

        //Creating segmentation data
        List<Integer> segs = segmentationData(ref, conv)

        //Loading parameters
        TevaParameters tevaParams = new TevaParameters(System.getResourceAsStream("/wsj.teva.properties"));

        //Getting working directory (creating base) from TevaParameters
        File base = new File(tevaParams.getWorkingDirectory())

        params.eachWithIndex { param, index ->

            File workingDir = createWorkingDirectory(base, name, index)

            new File(workingDir, "params.txt").withWriterAppend {
                it.println(param)
            }

            //Setting parameters
            tevaParams.setWorkingDirectory(workingDir.absolutePath)

            param.each { k, v ->

                if (k in tevaParams.stringPropertyNames()) {
                    tevaParams.setProperty(k, v as String)
                    log.info("Setting $k to $v")
                }

            }

            //Creating community model
            CommunityModel model = new MemoryBasedRunner(conv, tevaParams, new WSJTevaFactory(tevaParams, conv)).process();

            //gets slow here
            //Calculations
            TevaUtils.serialize(new File(tevaParams.getWorkingDirectory() + "/${conv.getName()}.${tevaParams.getFilenameIdentifier()}.xml"), model, CommunityModel.class);

            //Calculating pK
            def result = pk(segs, segment(model, conv))
            out.withWriterAppend {
                it.println "${name},${headers.collect() { tevaParams.getProperty(it) }.join(",")},${result.pMiss},${result.pFalseAlarm},${result.pk}"
            }
        }
    }

    /**
     * Initializes a Conversation based on the data file
     *
     * @param data
     * @return
     */
    def initConversation(File data) {

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
    def segmentationData(File ref, Conversation conv) {

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
                segdata << Long.parseLong((seg[0][0] as String).replaceAll(/\./, ""))
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
                } else {
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

    /**
     * Creates the working directory
     *
     * @param base
     * @param name
     * @param i
     * @return
     */
    static File createWorkingDirectory(File base, String name, int i) {
        File n = new File(base, "$name.$i")
        if (!n.exists()) {
            n.mkdir()
        } else {
            U.cleanDirectory(n)
        }
        n
    }

    /**
     * Segmenting
     *
     * @param model
     * @param conversation
     * @return
     */
    def static List segment(CommunityModel model, Conversation conversation) {

        def assignments = assign(model)
        smooth(conversation, assignments)

        def segs = []
        conversation.allThreads.each { t ->
            def lastid = -1
            t.posts.each { p ->
                if (assignments[p.postid]) {
                    def lidx = -1
                    assignments[p.postid].eachWithIndex { e, i ->
                        if (lidx < 0 || e > assignments[p.postid][lidx]) {
                            lidx = i
                        }
                    }
                    segs << ((lastid != lidx) ? 1 : 0)
                    lastid = lidx
                } else {
                    segs << 0
                }
            }
        }
        segs
    }

    /**
     *
     * @param model
     * @return
     */
    def static Map assign(CommunityModel model) {
        Map result = new HashMap()

        for (Community c : model.communities) {
            c.assignments.each { ConversationChunk chunk ->
                chunk.messages.each { Windowable w ->
                    if (!result[w.id]) {
                        result[w.id] = new Object[model.communities.size()]
                        Arrays.fill(result[w.id], 0f)
                    }
                    result[w.id][c.id as int] = chunk.coverage
                }
            }
        }
        result
    }

    /**
     * Loess smoother
     *
     * @param conversation
     * @param assignments
     * @param smooth
     * @return
     */
    def static smooth(Conversation conversation, Map assignments, smooth = 0.3d) {
        LoessInterpolator loess = new LoessInterpolator(smooth, 1)
        def x = []
        def length = assignments.values().first().length
        (0..<length).each { idx ->
            conversation.allThreads.each { t ->
                def data = t.posts.collect { p ->
                    if (x.size() < t.posts.size()) {
                        x << ((x && x.last() >= p.time.time) ? (x.last() + 1d) : (p.time.time as double))
                    }
                    assignments[p.postid] ? assignments[p.postid][idx] as double : 0d
                } as double[]
                data = loess.smooth(x as double[], data)
                t.posts.eachWithIndex { Post p, int i ->
                    if (assignments[p.postid]) assignments[p.postid][idx] = data[i]

                }
            }
        }
    }

    static void main(String[] args) {
        File WSJdir = U.getAnyFile("WSJ Directory", ".", JFileChooser.DIRECTORIES_ONLY)
        //File WSJdir = new File("/Users/charlesbean/Code/TEvA/Corpora/Converted/WSJ/Data")
        if (!WSJdir.isDirectory()) {
            println "Whoops! Not a directory"
        } else {
            new WSJImplementation(WSJdir)
        }
    }
}
