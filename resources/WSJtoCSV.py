################# WSJtoCSV ##################
##
## Charles Bean
## 3/25/14
##
#############################################

########### Global Parameters ###########

origin = "/Users/charlesbean/Code/TEvA/Corpora/lcseg 3/corpora/wsj" #THIS IS USED FOR RENAMING PURPOSES ONLY



rootdir = "/Users/charlesbean/Code/TEvA/Corpora/Renamed/ReWSJ" #THIS IS WHAT THE SCRIPT WILL RUN ON (transforms files in this dir/subdirs)

outdir = "/Users/charlesbean/Code/TEvA/Corpora/Converted/WSJ/Data/Extracted" #DIRECTORY FOR EXTRACTED FILES

refdir = "/Users/charlesbean/Code/TEvA/Corpora/Converted/WSJ/Data/Ref Files" #DIRECTORY FOR REFERENCE FILES



renamePath = "/Users/charlesbean/Code/TEvA/Corpora/Renamed/ReWSJ/" #DIRECTORY FOR RENAMED FILES (e.g. WSJ had .ref as suffix)

extension = ".exWSJ" #NEW EXTENSION/SUFFIX FOR RENAMED FILES

##########################################

import os
import sys
import string

__author__ = 'charlesbean'

class ConvertedICMI(object):

    def __init__(self):
        self.file = ""

    def __directory_check__(self, rootdir):
        """ Algorithm:
                1. Used for error checking
                2. Iterates through folders/files in directory, prints out
        """
        print "\n==========================\nDirectory: %s\nFiles taken:\n" % rootdir
        article_count = 0
        for subdir, dirs, files in os.walk(rootdir):
            for file in files:
                if file != ".DS_Store":
                    print " " + subdir + "/" + file
                    article_count += 1
        print "\nNumber of files: %s \n==========================\n" % article_count

    def iterate(self):
        for subdir, dirs, files in os.walk(rootdir):
            for file in files:
                if file != ".DS_Store":
                    self.file = subdir + "/" + file
                    self.process(outdir + "/" + file + ".csv", refdir + "/" + file + ".ref")

    def process(self, outFilename, refFilename):
        id = 0
        replyTo = -1
        start = 5 #Seconds
        author = "chucknorris"

        refFile = open(refFilename, "w")

        outFile = open(outFilename, "w")
        outFile.write("id\treplyTo\tstart\tauthor\ttext\n")

        with open(self.file, "r") as open_file:
            for line in open_file:
                if line != "==========\n":
                    if (replyTo == -1):
                        text = str(id) + '\t' + "" + '\t' + str(start) + '\t' + author + '\t' + line
                        id += 1
                        replyTo += 1
                        start += 5


                        outFile.write(text)
                        refFile.write(str(0) + '\n')
                    else:
                        text = str(id) + '\t' + str(replyTo) + '\t' + str(start) + '\t' + author + '\t' + line
                        id += 1
                        replyTo += 1
                        start += 5


                        outFile.write(text)
                        refFile.write(str(0) + '\n')

                else:
                    refFile.write(str(1) + '\n')

        refFile.close()
        outFile.close()


def rename_files(dir): #Yes this was necessary...
    print "\n==========================\nRenaming: %s...\n" % dir
    count = 1
    for subdir, dirs, files in os.walk(rootdir):
        for file in files:
            if file != ".DS_Store":
                os.rename(subdir+ "/"+ file, renamePath + str(count) + extension)
                count += 1

    print "\n==========================\nTotal files renamed: %d\n" % count

def main():
    #rename_files(rootdir)
    Converted = ConvertedICMI()
    Converted.__directory_check__(rootdir)
    Converted.iterate()


if __name__ == '__main__':
    main()


