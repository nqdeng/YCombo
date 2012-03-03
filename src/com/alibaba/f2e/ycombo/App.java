/**
 * YCombo
 * Copyright (c) 2012 Alibaba.com, Inc.
 * MIT Licensed
 * @author Nanqiao Deng
 */
package com.alibaba.f2e.ycombo;

import jargs.gnu.CmdLineParser;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Class for arguments parsing.
 */
public class App {
	/**
	 * Print error message and exit application.
	 * @param msg Error message.
	 */
	public static void exit(String msg) {
		System.err.println("\n[ERROR] " + msg);
		System.exit(1);
	}
	
	/**
	 * Print exception stack trace and exit application.
	 * @param e Exception object.
	 */
	public static void exit(Exception e) {
		e.printStackTrace();
		System.exit(1);
	}
	
	/**
	 * Application entrance.
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option verboseOpt = parser.addBooleanOption('v', "verbose");
        CmdLineParser.Option nomungeOpt = parser.addBooleanOption("nomunge");
        CmdLineParser.Option linebreakOpt = parser.addStringOption("line-break");
        CmdLineParser.Option preserveSemiOpt = parser.addBooleanOption("preserve-semi");
        CmdLineParser.Option disableOptimizationsOpt = parser.addBooleanOption("disable-optimizations");
        CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
        CmdLineParser.Option charsetOpt = parser.addStringOption("charset");
		CmdLineParser.Option rootOpt = parser.addStringOption("root");
		CmdLineParser.Option prefixOpt = parser.addStringOption("prefix");
		
		try {
			parser.parse(args);
			
			// Deal with -h, --help
			Boolean help = (Boolean) parser.getOptionValue(helpOpt);
			if (help != null && help.booleanValue()) {
				usage();
			}
			
			// Deal with -v, --verbose
			boolean verbose = parser.getOptionValue(verboseOpt) != null;
			
			// Deal with --charset <charset>
            String charset = (String) parser.getOptionValue(charsetOpt);
            if (charset == null || !Charset.isSupported(charset)) {
            	charset = "UTF-8";
                if (verbose) {
                    System.err.println("\n[INFO] Using default charset " + charset);
                }
            }
            
            // Deal with --line-break <column>
            int linebreakpos = -1;
            String linebreakstr = (String) parser.getOptionValue(linebreakOpt);
            if (linebreakstr != null) {
                try {
                    linebreakpos = Integer.parseInt(linebreakstr, 10);
                } catch (NumberFormatException e) {
                    usage();
                }
            }
            
            // Deal with --nomunge
            boolean munge = parser.getOptionValue(nomungeOpt) == null;
            
            // Deal with --preserve-semi
            boolean preserveAllSemiColons = parser.getOptionValue(preserveSemiOpt) != null;
            
            // Deal with --disable-optimizations
            boolean disableOptimizations = parser.getOptionValue(disableOptimizationsOpt) != null;
            
            // Deal with --root <folder>
            String root = (String) parser.getOptionValue(rootOpt);
            
            // Deal with --prefix <prefix>
            String prefix = (String) parser.getOptionValue(prefixOpt);
            if (prefix == null) {
            	// Extension name prefix of seed file defaults to "seed".
            	prefix = "seed";
            }
            
            // Deal with [input file]
            String[] input = parser.getRemainingArgs();
            if (input.length == 0) {
            	// If no input file, use current folder as input file by default.
            	input = new String[] { "." };
            }
            
            Compressor compressor = new Compressor(root, charset, prefix, linebreakpos, munge, verbose, preserveAllSemiColons, disableOptimizations);
            
            // Find all seed files for given input.
            ArrayList<File> seeds = new ArrayList<File>();
            for (String path : input) {
            	findSeed(new File(path), seeds, "." + prefix + ".js", "." + prefix + ".css");
            }
            
            // Compress each seed.
            for (File seed : seeds) {
            	System.err.println("\n[INFO] Processing " + seed.getAbsolutePath());
            	compressor.compress(seed);
            }
			
		} catch (CmdLineParser.OptionException e) {
			usage();
		}
	}
	
	/**
	 * Find seed files recursively for a given path. 
	 * @param f The starting path to find seeds.
	 * @param seeds List to fill with found seeds.
	 * @param jsExt Extension name of js seed file.
	 * @param cssExt Extension name of css seed file.
	 */
	private static void findSeed(File f, final List<File> seeds, final String jsExt, final String cssExt) {
		String name = f.getName().toLowerCase();
		if (f.isFile()) {
			if (name.endsWith(jsExt) || name.endsWith(cssExt)) {
				seeds.add(f);
			}
		// Skip meta-data folders such as ".svn".
		} else if (f.isDirectory() && (!name.startsWith(".") || name.equals(".") || name.equals(".."))) {
			for (File sub : f.listFiles()) {
				findSeed(sub, seeds, jsExt, cssExt);
			}
		}
	}
	
	/**
	 * Print help message and exit application.
	 */
	private static void usage() {
		String msg = ""
			+ "\nUsage: java -jar ycombo-x.y.z.jar [option] [input file]\n\n"
			
			+ "Global Options\n"
			+ "  -h, --help               Displays this information\n"
			+ "  --charset <charset>      Read the input file using <charset>, default to UTF-8\n"
			+ "  --line-break <column>    Insert a line break after the specified column number\n"
			+ "  -v, --verbose            Display informational messages and warnings\n\n"
			
			+ "JavaScript Options\n"
			+ "  --nomunge                Minify only, do not obfuscate\n"
			+ "  --preserve-semi          Preserve all semicolons\n"
			+ "  --disable-optimizations  Disable all micro optimizations\n\n"
			
			+ "Combo Options\n"
			+ "  --root <folder>          Specify the root folder of dependent files.\n"
			+ "  --prefix <prefix>        Specify the extension name prefix of seed file.\n"
			+ "                           It defaults to \"seed\" so seed file has a default\n"
			+ "                           extension name \".seed.js\" or \".seed.css\".\n\n"
			
			+ "If root folder is not specified, it defaults to workdir. If workdir is inside\n"
			+ "intl-style/xxx/htdocs, htdocs will be used as root folder instead.\n\n"
			
			+ "If no input file is specified, it defaults to workdir.";
		
		System.err.println(msg);
		System.exit(1);
	}
}
