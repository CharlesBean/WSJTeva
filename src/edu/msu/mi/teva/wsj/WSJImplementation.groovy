package edu.msu.mi.teva.wsj

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
         should consist of one folder of .csv files ("Extracted"), and \
         one folder of .ref files ("Ref Files")*/
    WSJImplementation(File dir){

        //Creating a map of processes
        def process_map = [:]
        int count = 0

        //Iterate through subdirectories, then files
        dir.eachFile { subdir ->
            print("---"+subdir.getName()+"---"+"\n") //REMOVE
            if (subdir.isDirectory()){
                subdir.eachFileMatch(FileType.FILES, ~/.*(?:csv|ref)/) { f ->
                    def prefix = (f.getName() =~ fileRegex)[0][1] //CHANGE THIS TO GET THE RENAMED EXTENSION
                    //print(prefix+"\n")
                    if (!process_map[prefix]) { //If prefix isnt within map, add key (value a map)
                        process_map[prefix] = [:]
                    }
                    if (f.getName().endsWith("ref")) {
                        process_map[prefix]["ref"] = f
                        print (process_map[prefix]["ref"])
                    }
                    count += 1
                }
            }

        }
        passed(count)
        //println ("Check at %d\n", getLineNumber())
        /*def process = [:]
        dir.eachFileMatch(FileType.FILES, ~/B.*(?:csv|ref)/) { f ->
            println("CHECK")
            def prefix = (f.getName() =~ /([^\.]+)/)[0][0]
            if (!process[prefix]){
                process[prefix] = [:]
            }
            if (f.getName().endsWith("ref")){
                process[prefix]["ref"] = f
            }
            else{
                process[prefix]["dat"] = f
            }
            print(process)
        }

        process.keySet().retainAll(process.findResults { (it.value["ref"] && it.value["dat"]) ? it.key : null})
        run(process)
        */
    }

    def passed(int count = 0){
        printf("--PASSED %d FILES--\n", count)
    }

    def run(Map data){
        File out = new File("WSJOutput.csv")
        out.withWriterAppend {
            it.println("corpus, window, delta, pmiss, pfa, pk")
        }
        data.each { String k, v ->
            runInstance(k, v["dat"] as File, v["ref"] as File, out)
        }
    }

    def runInstance(String name, File data, File ref, File out){
        pass
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
