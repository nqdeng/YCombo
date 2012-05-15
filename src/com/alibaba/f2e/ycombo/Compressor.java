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
public class Compressor extends Combiner {
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
	public Compressor(String root, String charset, String extname, int linebreakpos, boolean munge, boolean verbose, boolean preserveAllSemiColons, boolean disableOptimizations) {
		super(root, charset, extname);

		this.linebreakpos = linebreakpos;
		this.munge = munge;
		this.verbose = verbose;
		this.preserveAllSemiColons = preserveAllSemiColons;
		this.disableOptimizations = disableOptimizations;
	}
	
	/**
	 * Compress code from input by YUI Compressor and write to output.
	 * @overrides
	 * @param in Input stream reader.
	 * @param out Output stream writer.
	 */
	protected void refine(Reader in, Writer out) throws IOException, CombinerException {
		if (type.equals("js")) {
			compressJS(in, out);
		} else if (type.equals("css")) {
			compressCSS(in, out);
		}
	}
	
	/**
	 * Compress the given seed file as JavaScript.
	 * @param in Input reader.
	 * @param out Output writer.
	 */
	private void compressJS(Reader in, Writer out) throws IOException, CombinerException {
		try {
			new JavaScriptCompressor(in, new ErrorReporter() {
			    public void warning(String message, String sourceName,
			            int line, String lineSource, int lineOffset) {
			        if (line < 0) {
			            System.err.println("\n[WARNING] " + message);
			        } else {
			            System.err.println("\n[WARNING] " + line + ':' + lineOffset + ':' + message);
			        }
			        
			        if (lineSource != null) {
			        	System.err.println(lineSource);
			        }
			    }

			    public void error(String message, String sourceName,
			            int line, String lineSource, int lineOffset) {
			        if (line < 0) {
			            System.err.println("\n[ERROR] " + message);
			        } else {
			            System.err.println("\n[ERROR] " + line + ':' + lineOffset + ':' + message);
			        }

			        if (lineSource != null) {
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
			throw new CombinerException("");
		}
	}
	
	/**
	 * Compress the given seed file as CSS.
	 * @param in Input reader.
	 * @param out Output writer.
	 */
	private void compressCSS(Reader in, Writer out) throws IOException {
		new CssCompressor(in).compress(out, linebreakpos);
	}
}
