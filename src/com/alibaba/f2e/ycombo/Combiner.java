/**
 * YCombo
 * Copyright (c) 2012 Alibaba.com, Inc.
 * MIT Licensed
 * @author Nanqiao Deng
 */
package com.alibaba.f2e.ycombo;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Class for combining source files.
 */
public class Combiner {
	// Source file encoding.
	protected String charset;
	
	// Seed file extension name.
	protected String extname;
	
	// Type of seed file.
	protected String type;
	
	// Line between JS files.
	protected String SEPARATOR_JS;
	
	// Line between CSS files.
	protected String SEPARATOR_CSS;
	
	// SourceFile instance.
	protected SourceFile sourceFile;
	
	// Output buffer.
	private ByteArrayOutputStream buffer;
	
	public Combiner(String root, String charset, String extname) {
		this.charset = charset;
		this.extname = extname;
		
		SEPARATOR_JS = "\r\n;\r\n";
		SEPARATOR_CSS = "\r\n\r\n";
		
		sourceFile = new SourceFile(root, charset);
	}
	
	/**
	 * Process the given seed file.
	 * @param seed The seed file.
	 */
	public void process(File seed) throws SourceFileException, CombinerException {
		try {
			combine(seed);
		} catch (IOException e) {
			App.exit(e);
		}
	}
	
	/**
	 * Read data from input, refine it, and write to output.
	 * @param in Input stream reader.
	 * @param out Output stream writer.
	 */
	protected void refine(Reader in, Writer out) throws IOException, CombinerException {
		// In this class input data is simply piped to output. 
		int c;
		while ((c = in.read()) != -1) {
			out.write(c);
		}
	}
	
	/**
	 * Combine the given seed file.
	 * @param seed The seed file.
	 */
	private void combine(File seed) throws IOException, SourceFileException, CombinerException {
		String name = seed.getName();
		
		if (name.endsWith(".js." + extname)) {
			type = "js";
		} else if (name.endsWith(".css." + extname)) {
			type = "css";
		} else {
			throw new CombinerException("Cannot detect seed file type.");
		}
		
		LinkedHashMap<String, Reader> inputs = prepareInput(seed);
		Writer out = prepareOutput();
		Reader in = null;
		String file = null;
		
		try {
			for (Map.Entry<String, Reader> entry : inputs.entrySet()) {
				file = entry.getKey();
				in = entry.getValue();
				
				refine(in, out);
				
				in.close();
				in = null;
				
				// Insert empty lines betweens files to avoid the single-line comment
	    		// at the last line of the prev file mixing with the code
	    		// at the first line of the next file.
	    		// Insert an semicolon betweens JS files to avoid the bug
	    		// when a function expression that misses the semicolon at end
	    		// and followed by a parentheses.
	    		if (type.equals("js")) {
	    			out.write(SEPARATOR_JS);
	    		} else if (type.equals("css")) {
	    			out.write(SEPARATOR_CSS);
	    		}
			}
			
			out.flush();
			
			// Write output buffer to output file.
			writeFile(seed);
			
			out.close();
			out = null;
		} catch (CombinerException e) {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			System.err.println("in " + file);
			throw e;
		}
	}
	
	/**
	 * Concat seed file and all dependencies into a binary array then return the ByteArrayInputStream Reader.
	 * @param seed The seed file.
	 * @return The ByteArrayInputStream Readers of each input files.
	 */
	private LinkedHashMap<String, Reader> prepareInput(File seed) throws SourceFileException {
		ArrayList<String> output = sourceFile.combo(seed);
		LinkedHashMap<String, Reader> readers = new LinkedHashMap<String, Reader>();
		
		try {
			for (String path : output) {
				readers.put(path, new InputStreamReader(new ByteArrayInputStream(sourceFile.readBinary(path)), charset));
			}
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		}
		
		return readers;
	}
	
	/**
	 * Create an output buffer and return the Writer.
	 * @return The FileOutputStream Writer.
	 */
	private Writer prepareOutput() {
		Writer w = null;
		
		try {
			buffer = new ByteArrayOutputStream();
			w = new OutputStreamWriter(buffer, charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		}
		
		return w;
	}
	
	/**
	 * Map buffer to file.
	 * @param seed The seed file.
	 * @param out The output buffer.
	 */
	private void writeFile(File seed) throws IOException {
		try {
			// Output file locates in the same folder,
			// and has the same name with the seed file but a different extension name.
			FileOutputStream file = new FileOutputStream(seed.getAbsolutePath().replaceAll("\\." + extname + "$", ""));
			buffer.writeTo(file);
			file.flush();
			file.close();
		} catch (FileNotFoundException e) {
			App.exit(e);
		}
	}
}