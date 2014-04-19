package edu.msu.mi.teva.wsj

import groovy.io.FileType
import edu.msu.mi.teva.wsj.WSJRunner

/**
 * Created by charlesbean on 3/25/14.
 */
class WSJLooper {

    public static void run(){
        def list = []

        def dir = new File("/Users/charlesbean/Code/TEvA/Corpora/Converted/WSJ/Extracted")
        dir.eachFileRecurse (FileType.FILES) { file ->
            list << file
        }

        list.each{
            if (it.path != "/Users/charlesbean/Code/TEvA/Corpora/Converted/WSJ/Extracted/.DS_Store"){
                println it
                WSJRunner.run(it)
            }
        }
    }

    public static void main(String[] args){
        run()
    }
}
