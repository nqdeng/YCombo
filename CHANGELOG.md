2012.02.13, Version 0.1.0
-------------------------
* initial release

2012.03.02, Version 0.1.1
-------------------------
* Fix a bug when source file is not end with new line character.

2012.03.03, Version 0.1.2
-------------------------
* Fix the incorrect output filename when input filename contains dot.

2012.03.13, Version 0.1.3
-------------------------
* Change seed file extension name from seed.TYPE to TYPE.seed to avoid conflict with Unicorn.
* Change common-line option "--prefix" to "--extname" to response to the change above.

2012.03.18, Version 0.1.4
-------------------------
* Fix the log when input file has syntax error.
* Error in one seed file will not break the whole combo task now.
* Add new common-line option "--nocompress", allowing to combine file only.

2012.03.30, Version 0.1.5
-------------------------
* Auto trim head & tail whitespace of #require instruction line for improving fault-tolerant.

2012.04.25, Version 0.1.6
-------------------------
* Add a semicolon between files that combined together to avoid the known semicolon bug.

2012.04.27, Version 0.1.7
-------------------------
* Fix the bug wrongly adding a semicolon between css files.

2012.05.15, Version 0.1.8
-------------------------
* Adjust file compression method. Now every required file is compressed independently and outputted one line per file.
