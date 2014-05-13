################# WSJtoCSV ##################
##
## Charles Bean
## 3/25/14
##
##
#############################################

"""
-----> PLEASE READ!!!
    Please run with Python 2.7.

    Please add a forward-slash at the end of each directory. (See below)

    Also, please use the "rename_files" function to rename the file extension (simply uncomment below). The
        extensions ".ref" or ".csv" are not suitable for the TEvA module (it converts them itself).

    My directory structure:
        "Corpora"
            -"Converted"
                -"WSJ"
                    -"Data"
                        -"Extracted"            <------ outdir
                        -"Ref Files"            <------ refdir
                    -"Extra"
            -"lcseg 3"
                -"corpora"
                    -"wsj"                      <------ originalDir
            -"Renamed"
                -"ReWSJ"                        <------ rootdir

"""

########### Global Parameters ###########

"""Ouput"""

#Directory the script will operate on
rootdir = "/Users/josh/@Projects/KNOWLEDGE_FLOW/Validation/WSJ/input/"

#Directory data will be outputted to (.csv)
outdir = "/Users/josh/@Projects/KNOWLEDGE_FLOW/Validation/WSJ/output/data/"

#Directory reference files will be outputted to (.ref)
refdir = "/Users/josh/@Projects/KNOWLEDGE_FLOW/Validation/WSJ/output/ref/"



"""Renaming"""

#Copies files from this directory to "rootdir" - gets rid of ".ref" extension
originalDir = "/Users/josh/@Projects/KNOWLEDGE_FLOW/lcseg/corpora/wsj/"

#New extension (for rename_files function)
extension = ".exWSJ" #New extension/suffix for renaming files

##########################################

import os
import sys
import string
import shutil
import re

__author__ = 'charlesbean'

class ConvertedWSJ(object):

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
                    self.process(outdir + file + ".csv", refdir + file + ".ref")

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

def rename_files(dir): #Copy files to rootdir, and rename (according to extension)

    """rootdir is the rename destination directory (for some reason the iteration through
    subdirectories is weird but shouldnt be worried about)"""

    #total file count (for WSJ should be 500)
    total = 0

    #COPY and RENAME
    print "\n==========================\nCopying files from {} to {}...\n".format(originalDir, rootdir)

    for subdir, dirs, files in os.walk(dir):
        for file in files:
            if ((file != ".DS_Store") and (file != "CVS") and (file[-4:] == (".ref"))):
                total += 1
                origName = file #saving original name for printing

                shutil.copy(subdir + "/" + file, rootdir + str(total) + extension)

                //os.rename(rootdir + file, rootdir + str(total) + extension)

                print "\tCopied file: {} to \n\t\t {}".format(file, rootdir)
                print "\t\t\t and renamed it to: {} ".format((str(total) + extension))

    print "\n============ Total files copied and renamed: %d ==============\n" % total




def main():
    rename_files(originalDir)
    Converted = ConvertedWSJ()
    Converted.__directory_check__(rootdir)
    Converted.iterate()


if __name__ == '__main__':
    main()


