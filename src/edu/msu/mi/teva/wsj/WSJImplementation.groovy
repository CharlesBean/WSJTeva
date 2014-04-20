package edu.msu.mi.teva.wsj

import edu.mit.cci.teva.model.Conversation
import groovy.io.FileType
import edu.mit.cci.util.U

import javax.swing.JFileChooser


/**
 * Created by charlesbean on 4/18/14.
 */
class WSJImplementation {

    //Setting a couple of different parameters (based off of ICMI currently)
    def params = [[window: 50, delta: 10], [window: 100, delta: 10], [window: 100, delta: 20], [window: 120, delta:30]]

    //Regex expression for file prefixes (1.exWSJ.ref/1.exWSJ.csv)
    def fileRegex = ~/(.*)(?:.csv|.ref)/  //Full name at [0][0]; Name without extension at [0][1]

    /*Implementing the WSJ module over a directory of corpora ("Data") - it
         should consist of one folder of .csv files ("Extracted"), and
         one folder of .ref files ("Ref Files")*/
    WSJImplementation(File dir){

        //Creating a map of processes
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
                        process_map[prefix]["dat"] = f //If .csv, add to submap [0]
                    }
                    count += 1
                }
            }
        }
        process_map.keySet().retainAll(process_map.findResults { (it.value["ref"] && it.value["dat"]) ? it.key : null })

        passed(count)

        run(process_map)
    }

    //For systematic checking
    def passed(int count = 0){
        printf("--PASSED %d FILES--\n", count)
    }

    def run(Map data){
        File out = new File("WSJOutput.csv") //Final output file
        out.withWriterAppend {
            it.println("corpus, window, delta, pmiss, pfa, pk") //Write columns
        }
        data.each { String k, v -> //For each file prefix (k), and each ref/csv file (v[csv] or v[ref])
            runInstance(k, v["dat"] as File, v["ref"] as File, out)
        }
    }

    def runInstance(String name, File data, File ref, File out){

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
