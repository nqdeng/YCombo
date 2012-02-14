**This application is mainly used in Alibaba.com F2E Team, but it should work for anyone else.**

YCombo
======

*A JavaScript/CSS source file combinator.*

Introduction
------------

Alibaba.com uses a source file dependencies management and combination service which named Unicorn, but sometimes we F2E Team need to combo files with their dependencies into a single file offline and use it directly online. So we need an offline combo tool and we made this YCombo. The "Y" in "YCombo" is honored with the [YUI Compressor](http://developer.yahoo.com/yui/compressor/) which is used as the compressor lib.

Usage
-----

Since YCombo wraps YUI Compressor inside, most command-line arguments of YUI Compressor are available to YCombo as below:

	Usage: java -jar ycombo-x.y.z.jar [option] [input file]
				
	Global Options
	  -h, --help               Displays this information.
	  --charset <charset>      Read the input file using <charset>, default to UTF-8.
	  --line-break <column>    Insert a line break after the specified column number.
	  -v, --verbose            Display informational messages and warnings.
				
	JavaScript Options
	  --nomunge                Minify only, do not obfuscate.
	  --preserve-semi          Preserve all semicolons.
	  --disable-optimizations  Disable all micro optimizations.
				
	Combo Options
	  --root <folder>          Specify the root folder of dependent files.
	  --prefix <prefix>        Specify the extension name prefix of seed file.
	                           It defaults to "seed" so seed file has a default
	                           extension name ".seed.js" or ".seed.css".
				
	If root folder is not specified, it defaults to workdir. If workdir is inside
	intl-style/xxx/htdocs, htdocs will be used as root folder instead.
				
	If no input file is specified, it defaults to workdir.

Features special to YCombo are detailed below.

### Dependencies Management

YCombo supports source file dependencies management. YCombo combines files starting from a seed file. A seed file could be a JS file or CSS file, and using one of the follow special one-line comments to specify its dependent files.

	// #require <PATH>
	// #require "PATH"
	/* #require <PATH> */
	/* #require "PATH" */

Both single-line and multi-line style could be used in JS file while only multi-line style could be used in CSS file. PATH wrapped in <> is related to the Root Folder, while PATH wrapped in "" is related to the file which requires others.

Required file could continue requiring other files by the same approach, which finally results a dependency tree grown from the seed.

YCombo travels the dependency tree and calculates the file combination order by DFS and Post-Order algorithm. So if a dependency tree is like this:

	         SEED
	          /\
	         A  B
	        /   /\
	       C   C  D
	      /   /   /\
	     E   E   F  G

The file combination order will be:

	E > C > A > F > G > D > B > SEED

This ensures the right execution order for JS and CSS files. File required more than once also appears at the right place only once in the list. Circular dependency could also be detected during the travel.

### Root Folder

The root folder which the paths of dependent files relate to. During one combination task all input files and their dependencies can only share one root folder. For example, consider the follow two files:

	/foo/bar/baz/a.js
	/foo/bar/b.js

if `a.js` requires `b.js`, we could write the follow comment in `a.js`:

	// #require "../b.js"

If we use `/foo/bar` as the root folder, we could alternatively write this:

	// #require <b.js>

Because Unicorn currently resolve dependencies using the <> style, so we support this way in YCombo. And since YCombo is a dev-tool for Alibaba.com first, YCombo will automatic detect the root folder(which is the htdocs folder) used in Alibaba.com by checking the directory structure if root folder is not specified explicitly from the command-line arguments. For the usage outside Alibaba.com, we recommend the "" style because it's more easy and flexible.

### Seed Extension Name Prefix

If one of the inputs from command-line arguments is a folder, YCombo will travel the whole folder and find all seed files to combine by its extension name. Extension name of seed files is prefix with `seed` by default, so the full extension name becomes `.seed.js` or `.seed.css`.

You could use different prefix to distinguish different type of seed files. Doing so benefits you from allowing to use a folder as the input but only combining a special subset of seed files inside.

### Sources Compression

After seed file and its dependencies all put together, YCombo uses YUI Compressor to compress the source code and product the final output. The output file has the same name with the seed file but the original extension name, and locates in the same folder of the seed file. For example, a.seed.js products a.js in the same folder.

License
-------

YCombo uses binary library and source code from YUI Compressor, which is released under the BSD (revised) license:

>Copyright (c) 2011 Yahoo! Inc.  All rights reserved.
>The copyrights embodied in the content of this file are licensed
>by Yahoo! Inc. under the BSD (revised) open source license.

YCombo is released under the MIT license:

>Copyright (c) 2012 Alibaba.Com, Inc.
>
>Permission is hereby granted, free of charge, to any person obtaining a copy of
>this software and associated documentation files (the "Software"), to deal in
>the Software without restriction, including without limitation the rights to
>use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
>of the Software, and to permit persons to whom the Software is furnished to do
>so, subject to the following conditions:
>
>The above copyright notice and this permission notice shall be included in all
>copies or substantial portions of the Software.
>
>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
>IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
>FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
>AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
>LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
>OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
>SOFTWARE.