/**
 * YUI Compressor
 * http://developer.yahoo.com/yui/compressor/
 * Author: Julien Lecomte -  http://www.julienlecomte.net/
 * Copyright (c) 2011 Yahoo! Inc.  All rights reserved.
 * The copyrights embodied in the content of this file are licensed
 * by Yahoo! Inc. under the BSD (revised) open source license.
 */

/**
 * YCombo
 * Copyright (c) 2012 Alibaba.com, Inc.
 * MIT Licensed
 * @author Nanqiao Deng
 */
package com.alibaba.f2e.ycombo;

import org.mozilla.javascript.*;
import com.yahoo.platform.yui.compressor.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Class for wrapping YUI Compressor.
 */
public class Compressor {
	// Source file encoding.
	private String charset;
	
	// Insert a line break after the specified column number.
	private int linebreakpos;
	
	// Minify only, do not obfuscate.
	private boolean munge;
	
	// Display informational messages and warnings.
	private boolean verbose;
	
	// Preserve all semicolons.
	private boolean preserveAllSemiColons;
	
	// Disable all micro optimizations.
	private boolean disableOptimizations;
	
	// SourceFile instance.
	private SourceFile sourceFile;
	
	// New line character.
	private byte[] NEW_LINE;
	
	/**
	 * Create a new Compressor instance with options.
	 * @param root Root path specified from command line.
	 * @param charset Text encoding of source file.
	 * @param linebreakpos Insert a line break after the specified column number.
	 * @param munge Minify only, do not obfuscate.
	 * @param verbose Display informational messages and warnings.
	 * @param preserveAllSemiColons Preserve all semicolons.
	 * @param disableOptimizations Disable all micro optimizations.
	 */
	public Compressor(String root, String charset, int linebreakpos, boolean munge, boolean verbose, boolean preserveAllSemiColons, boolean disableOptimizations) {
		this.charset = charset;
		this.linebreakpos = linebreakpos;
		this.munge = munge;
		this.verbose = verbose;
		this.preserveAllSemiColons = preserveAllSemiColons;
		this.disableOptimizations = disableOptimizations;
		
		try {
			this.NEW_LINE = "\r\n".getBytes(this.charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		}
		
		sourceFile = new SourceFile(root, charset);
	}
	
	/**
	 * Compress the given seed file.
	 * @param seed The seed file.
	 */
	public void compress(File seed) {
		try {
			String name = seed.getName();
			String type = null;
			
			if (name.endsWith(".js")) {
				type = "js";
			} else if (name.endsWith(".css")) {
				type = "css";
			} else {
				App.exit("Cannot detect file type of " + name);
			}
			
			Reader in = prepareInput(seed);
			Writer out = prepareOutput(seed, type);

			if (type.equals("js")) {
				compressJS(in, out);
			} else if (type.equals("css")) {
				compressCSS(in, out);
			}
			
			in.close();
			out.close();
		} catch (IOException e) {
			App.exit(e);
		}
	}
	
	/**
	 * Compress the given seed file as JavaScript.
	 * @param in Input reader.
	 * @param out Output writer.
	 */
	private void compressJS(Reader in, Writer out) {
		try {
			new JavaScriptCompressor(in, new ErrorReporter() {
			    public void warning(String message, String sourceName,
			            int line, String lineSource, int lineOffset) {
			        if (line < 0) {
			            System.err.println("\n[WARNING] " + message);
			        } else {
			            System.err.println("\n[WARNING] " + line + ':' + lineOffset + ':' + message);
			        }
			    }

			    public void error(String message, String sourceName,
			            int line, String lineSource, int lineOffset) {
			        if (line < 0) {
			            System.err.println("\n[ERROR] " + message);
			        } else {
			            System.err.println("\n[ERROR] " + line + ':' + lineOffset + ':' + message);
			            System.err.println(lineSource);
			        }
			    }

			    public EvaluatorException runtimeError(String message, String sourceName,
			            int line, String lineSource, int lineOffset) {
			        error(message, sourceName, line, lineSource, lineOffset);
			        return new EvaluatorException(message);
			    }
			}).compress(out, linebreakpos, munge, verbose, preserveAllSemiColons, disableOptimizations);
		} catch (EvaluatorException e) {
			e.printStackTrace();
            // Return a special error code used specifically by the web front-end.
            System.exit(2);
		} catch (IOException e) {
			App.exit(e);
		}
	}
	
	/**
	 * Compress the given seed file as CSS.
	 * @param in Input reader.
	 * @param out Output writer.
	 */
	private void compressCSS(Reader in, Writer out) {
		try {
			new CssCompressor(in).compress(out, linebreakpos);
		} catch (IOException e) {
			App.exit(e);
		}
	}
	
	/**
	 * Concat seed file and all dependencies into a binary array then return the ByteArrayInputStream Reader.
	 * @param seed The seed file.
	 * @return The ByteArrayInputStream Reader.
	 */
	private Reader prepareInput(File seed) {
		ArrayList<String> output = sourceFile.combo(seed);
		Reader reader = null;
		
		int len = 0;
    	for (String path : output) {
    		len += sourceFile.read(path).length;
    		len += this.NEW_LINE.length;
    	}
    	
    	ByteBuffer buffer = ByteBuffer.allocate(len);
    	for (String path : output) {
    		buffer.put(sourceFile.read(path));
    		buffer.put(this.NEW_LINE);
    	}
    	
    	try {
    		reader = new InputStreamReader(new ByteArrayInputStream(buffer.array()), this.charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		}
		
		return reader;
	}
	
	/**
	 * Open the output file then return the Writer.
	 * @param seed The seed file.
	 * @param type The type of seed file.
	 * @return The FileOutputStream Writer.
	 */
	private Writer prepareOutput(File seed, String type) {
		Writer w = null;
		
		try {
			// Output file locates in the same folder,
			// and has the same name with the seed file but a different extension name.
			w = new OutputStreamWriter(new FileOutputStream(seed.getAbsolutePath().replaceAll("(?:\\.\\w+)*$", "") + "." + type), this.charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		} catch (FileNotFoundException e) {
			App.exit(e);
		}
		
		return w;
	}
}
