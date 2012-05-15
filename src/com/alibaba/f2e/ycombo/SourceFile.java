/**
 * YCombo
 * Copyright (c) 2012 Alibaba.com, Inc.
 * MIT Licensed
 * @author Nanqiao Deng
 */
package com.alibaba.f2e.ycombo;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/**
 * Class for source file manipulation.
 */
public class SourceFile {
	// Cache binary data of source files by canonical path.
	private HashMap<String, byte[]> binaryCache;
	
	// Cache dependencies of source files by canonical path.
	private HashMap<String, ArrayList<String>> dependenceMap;
	
	// RegExp pattern to match #require statement.
	private Pattern PATTERN_REQUIRE;
	
	// Root folder of required file.
	private String root;
	
	// Text encoding of source files.
	private String charset;
	
	/**
	 * Create a new SourceFile instance with specified rootPath, file encoding and file type.
	 * @param root Root path specified from command line.
	 * @param charset Text encoding of source file.
	 */
	public SourceFile(String root, String charset) {
		// Initiation.
		binaryCache = new HashMap<String, byte[]>();
		dependenceMap = new HashMap<String, ArrayList<String>>();
		
		// Match "// #require <path>" or "// #require "path"" or "/* #require <path> */" or "/* #require "path" */".
		PATTERN_REQUIRE = Pattern.compile("^\\s*/[/\\*]\\s#require\\s([\"<])([\\w\\-\\./]+)[\">](?:\\s\\*/)?\\s*$", Pattern.MULTILINE);
		
		locateRoot(root);
		
		this.charset = charset;
	}
	
	/**
	 * Combine seed file with its' dependencies.
	 * @param seed The seed file.
	 * @return Output queue with correct dependencies order.
	 */
	public ArrayList<String> combo(File seed) throws SourceFileException {
		Stack<ArrayList<String>> tree = new Stack<ArrayList<String>>();
		ArrayList<String> root = new ArrayList<String>();
		ArrayList<String> output = new ArrayList<String>();
		
		// Construct the initial tree.
		root.add(canonize(seed));
		tree.add(root);
		
		// Travel the dependencies tree from the seed file.
		travel(tree, new Stack<String>(), output);
		
		return output;
	}
	
	/**
	 * Get binary data of a source file.
	 * @param path The canonical path of source file.
	 * @return Source file data.
	 */
	public byte[] readBinary(String path) throws SourceFileException {
		if (!binaryCache.containsKey(path)) {
			try {
				BufferedInputStream bf = new BufferedInputStream(new FileInputStream(new File(path)));
				try {
					byte[] data = new byte[bf.available()];
					bf.read(data);
					detectBOM(data, path);
					binaryCache.put(path, extractDependencies(data, path));
				} finally {
					bf.close();
				}
			} catch (IOException e) {
				App.exit(e);
			}
		}
		
		return binaryCache.get(path);
	}
	
	/**
	 * Get text content of a source file.
	 * @param path The canonical path of source file.
	 * @param charset Source file encoding.
	 * @return Source file content.
	 */
	public String readString(String path) throws SourceFileException {
		return decode(readBinary(path), path);
	}
	
	/**
	 * Get the canonical path of a file.
	 * @param f The file.
	 * @return The canonical path.
	 */
	private String canonize(File f) {
		String path = null;
		
		try {
			path = f.getCanonicalPath();
		} catch (IOException e) {
			App.exit(e);
		}
		
		return path;
	}
	
	/**
	 * Decode binary data to string.
	 * @param data Binary data of file.
	 * @param path Path of file.
	 * @return The decoded string.
	 */
	private String decode(byte[] data, String path) throws SourceFileException {
		String content = null;
		
		try {
			content = Charset.forName(charset).newDecoder().decode(ByteBuffer.wrap(data)).toString();
		} catch (CharacterCodingException e) {
			throw new SourceFileException("Cannot read " + path + " as " + charset + " encoded file");
		}
		
		return content;
	} 
	
	/**
	 * Detect Unicode BOM in a source file.
	 * @param data Binary data of source file.
	 * @param path The canonical path of source file.
	 */
	private void detectBOM(byte[] data, String path) throws SourceFileException {
		if (data.length > 2 && (byte)(data[0] ^ 0xEF) == 0 && (byte)(data[1] ^ 0xBB) == 0 && (byte)(data[2] ^ 0xBF) == 0) {
			throw new SourceFileException("UTF8 BOM was found in " + path);
		}
		else if (data.length > 1 && (byte)(data[0] ^ 0xFE) == 0 && (byte)(data[1] ^ 0xFF) == 0) {
			throw new SourceFileException("UTF16BE BOM was found in " + path);
		}
		else if (data.length > 1 && (byte)(data[0] ^ 0xFF) == 0 && (byte)(data[1] ^ 0xFE) == 0) {
			throw new SourceFileException("UTF16LE BOM was found in " + path);
		}
	}
	
	/**
	 * Extract dependencies information from input file.
	 * @param data Binary data of input file.
	 * @param path Path of input file.
	 * @return Binary data of input file that excludes the dependencies comments.
	 */
	private byte[] extractDependencies(byte[] data, String path) throws SourceFileException {
		ArrayList<String> dependencies = new ArrayList<String>();
		Matcher m = PATTERN_REQUIRE.matcher(decode(data, path));
		
		while (m.find()) {
			// Decide which root path to use.
			// Path wrapped in <> is related to root path.
			// Path wrapped in "" is related to parent folder of the source file.
			String root = null;
			
			if (m.group(1).equals("<")) {
				root = this.root;
			} else {
				root = new File(path).getParent();
			}
			
			// Get path of required file.
			String required = m.group(2);
			
			File f = new File(root, required);
			
			if (f.exists()) {
				dependencies.add(canonize(f));
			} else {
				throw new SourceFileException("Cannot find required file " + required + " in " + path);
			}
		}
		
		dependenceMap.put(path, dependencies);
		
		// Remove dependencies comments from input file.
		try {
			data = m.replaceAll("").getBytes(charset);
		} catch (UnsupportedEncodingException e) {
			App.exit(e);
		}
		
		return data;
	}
	
	/**
	 * Get dependencies of a source file.
	 * @param path The canonical path of source file.
	 * @return Path of dependencies. 
	 */
	private ArrayList<String> getDependencies(String path) throws SourceFileException {
		if (!dependenceMap.containsKey(path)) {
			readBinary(path);
		}
		
		return dependenceMap.get(path);
	}
	
	/**
	 * Locate root path.
	 * @param root The user-specified root path.
	 */
	private void locateRoot(String root) {
		// Locate default root folder.
		if (root == null) {
			File pwd = new File(".").getAbsoluteFile();
			File f = pwd;
			String[] l = null;
	
			// Detect intl-style/xxx/htdocs by finding "js" and "css" in sub folders.
			do {
				f = f.getParentFile();
				if (f == null) {
					break;
				}
				
				l = f.list(new FilenameFilter() {
					private Pattern pattern = Pattern.compile("^(?:js|css)$");
	
					public boolean accept(File dir, String name) {
						return pattern.matcher(name).matches();
					}
				});
			} while (l.length != 2);
			
			// If present, use intl-style/xxx/htdocs as root folder for Alibaba.
			if (f != null) {
				this.root = canonize(f);
			// Else use present working folder as root folder.
			} else {
				this.root = canonize(pwd);
			}
		// Use user-specified root folder.
		} else {
			File f = new File(root);
			if (f.exists()) {
				this.root = canonize(f);
			} else {
				App.exit("The user-specified root folder " + root + " does not exist.");
			}
		}
	}
	
	/**
	 * Travel dependencies tree by DFS and Post-Order algorithm.
	 * @param tree The initial tree which contains root node only.
	 * @param footprint The footprint of the traversal.
	 * @param output Output queue of combined files.
	 */
	private void travel(Stack<ArrayList<String>> tree, Stack<String> footprint, ArrayList<String> output) throws SourceFileException {
		for (String node : tree.peek()) {
			// Detect circular dependences by looking back footprint.
			if (footprint.contains(node)) {
				String msg = "Circular dependences was found\n";
				for (String path : footprint) {
					msg += "    " + path + " ->\n";
				}
				msg += "    " + node;
				throw new SourceFileException(msg);
			}
			
			// Skip visited node.
			if (output.contains(node)) {
				continue;
			}
			
			// Move forward.
			footprint.push(node);
			
			// Add sub nodes.
			tree.push(getDependencies(node));
			
			// Travel sub nodes.
			travel(tree, footprint, output);
			
			// Clean visited nodes.
			tree.pop();
			
			// Move backward.
			footprint.pop();
			
			// Add first visited node to output queue.
			output.add(node);
		}
	}
}
