WSJTeva
==============

This is a module for the topic evolution algorithm, TEvA. It applies the algorithm
to a WSJ (Wall Street Journal) Corpus [1], looking at the text from a conversational
perspective.

The corpus was made available by Michael Galley (mgalley@acm.org).

Dependencies
--------------

This module relies on:
    -TEvA   (available at: https://github.com/jintrone/TEvA)
    -cos    (available at: http://sourceforge.net/p/cosparallel/wiki/Home/)
    -maximal cliques (install from cos)

It also relies on the usage of the Python script "WSJtoCSV.py", located in the
resources folder. Use this script on the WSJ Corpus first to clean it (and
produce the CSV and Reference files), then this module can be applied. It is
important the WSJ files are also copied and renamed in a new directory (see below).

Usage
--------------

Renaming and copying the WSJ files to a new directory is default.

Please first run the "WSJtoCSV.py" script on the WSJ corpus first (located
in the resources folder) to clean the files, and create reference/csv files.
    * NOTE: It is recommended to rename the files first (using the "rename_files" function),
         *  as the default WSJ files end in ".ref". This will copy the files to a
         *  new directory, then rename them with the given extension (e.g. exWSJ, WSJ, etc.)

You will need:
    - A directory for csv files
    - A directory for ref files
    - A root directory
    - (Optional) Rename origin directory (directory that needs renaming)

Contact
--------------

WSJ Module and WSJtoCSV
    - Charles Bean
    - beanchar@msu.edu

TEvA
    - Joshua Introne
    - jintrone@msu.edu

References
-------------

[1] Michael Galley, et. al., "Discourse Segmentation of Multi-Party Conversation", Proceedings of the 41st Annual Meeting of the Association for Computational Linguistics, July 2003, pp. 562-569.