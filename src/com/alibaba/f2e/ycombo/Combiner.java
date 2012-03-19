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
	
	// New line character.
	protected byte[] NEW_LINE;
	
	// SourceFile instance.
	protected SourceFile sourceFile;
	
	public Combiner(String root, String charset, String extname) {
		this.charset = charset;
		this.extname = extname;
		
		try {
			this.NEW_LINE = "\r\n".getBytes(this.charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		}
		
		this.sourceFile = new SourceFile(root, charset);
	}
	
	/**
	 * Process the given seed file.
	 * @param seed The seed file.
	 */
	public void process(File seed) throws SourceFileException, CombinerException {
		combine(seed);
	}
	
	/**
	 * Combine the given seed file.
	 * @param seed The seed file.
	 */
	private void combine(File seed) throws SourceFileException, CombinerException {
		try {
			String name = seed.getName();
			String type = null;
			int c;
			
			if (name.endsWith(".js." + this.extname)) {
				type = "js";
			} else if (name.endsWith(".css." + this.extname)) {
				type = "css";
			} else {
				throw new CombinerException("Cannot detect seed file type.");
			}
			
			Reader in = prepareInput(seed);
			Writer out = prepareOutput(seed);

			while ((c = in.read()) != -1) {
				out.write(c);
			}
			out.flush();
			
			in.close();
			out.close();
		} catch (IOException e) {
			App.exit(e);
		}
	}
	
	/**
	 * Concat seed file and all dependencies into a binary array then return the ByteArrayInputStream Reader.
	 * @param seed The seed file.
	 * @return The ByteArrayInputStream Reader.
	 */
	protected Reader prepareInput(File seed) throws SourceFileException {
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
    		// Insert an empty line betweens files to avoid the single-line comment
    		// at the last line of the prev file mixing with the code
    		// at the first line of the next file.
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
	 * @return The FileOutputStream Writer.
	 */
	protected Writer prepareOutput(File seed) {
		Writer w = null;
		
		try {
			// Output file locates in the same folder,
			// and has the same name with the seed file but a different extension name.
			w = new OutputStreamWriter(new FileOutputStream(seed.getAbsolutePath().replaceAll("\\." + this.extname + "$", "")), this.charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		} catch (FileNotFoundException e) {
			App.exit(e);
		}
		
		return w;
	}
}