/*
 * Copyright (C) 2001 Clarkware Consulting, Inc.
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of Clarkware Consulting, Inc. nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without prior written permission. For written
 *     permission, please contact clarkware@clarkware.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CLARKWARE CONSULTING OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN  ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package utilMDE;

import java.io.*;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * <code>JWhich</code> is a utility that takes a Java class name
 * and displays the absolute pathname of the class file that would
 * be loaded first by the class loader, as prescribed by the
 * class path.
 * <p>
 * <code>JWhich</code> also validates the class path and reports
 * any non-existent or invalid class path entries.
 * <p>
 * Usage is similar to the UNIX <code>which</code> command.
 * <p>
 * Example uses:
 * <p>
 * <blockquote>
 * 		To find the absolute pathname of <code>MyClass.class</code>
 *		not in a package:
 *		<pre>java JWhich MyClass</pre>
 *
 * 		To find the absolute pathname of <code>MyClass.class</code>
 *		in the <code>my.package</code> package:
 *		<pre>java JWhich my.package.MyClass</pre>
 * </blockquote>
 *
 * @author <a href="mailto:mike@clarkware.com">Mike Clark</a>
 * @author <a href="http://www.clarkware.com">Clarkware Consulting, Inc.</a>
 */

public class JWhich {

	private static String CLASSPATH;

	/**
	 * Prints the absolute pathname of the class file
	 * containing the specified class name, as prescribed
	 * by the class path.
	 *
	 * @param className Name of the class.
	 */
	public static void which(String className) {

		URL classUrl = findClass(className);

		if (classUrl == null) {
			System.out.println("\nClass '" + className +
				"' not found.");
		} else {
			System.out.println("\nClass '" + className +
				"' found in \n'" + classUrl.getFile() + "'");
		}

		validate();

		printClasspath();
	}

	/**
	 * Returns the URL of the resource denoted by the specified
	 * class name, as prescribed by the class path.
	 *
	 * @param className Name of the class.
	 * @return Class URL, or null of the class was not found.
	 */
	public static URL findClass(final String className) {
		return JWhich.class.getResource(asResourceName(className));
	}

	protected static String asResourceName(String resource) {
		if (!resource.startsWith("/")) {
			resource = "/" + resource;
		}
		resource = resource.replace('.', '/');
		resource = resource + ".class";
		return resource;
	}

	/**
	 * Validates the class path and reports any non-existent
	 * or invalid class path entries.
	 * <p>
	 * Valid class path entries include directories, <code>.zip</code>
	 * files, and <code>.jar</code> files.
	 */
	public static void validate() {

		StringTokenizer tokenizer =
			new StringTokenizer(getClasspath(), File.pathSeparator);

		while (tokenizer.hasMoreTokens()) {
			String element = tokenizer.nextToken();
			File f = new File(element);

			if (!f.exists()) {
				System.out.println("\nClasspath element '" +
					element + "' " + "does not exist.");
			} else if ( (!f.isDirectory()) &&
					  (!element.toLowerCase().endsWith(".jar")) &&
					  (!element.toLowerCase().endsWith(".zip")) ) {

				System.out.println("\nClasspath element '" +
					element + "' " +
					"is not a directory, .jar file, or .zip file.");

			}
		}
	}

	public static void printClasspath() {

		System.out.println("\nClasspath:");
		StringTokenizer tokenizer =
			new StringTokenizer(getClasspath(), File.pathSeparator);
		while (tokenizer.hasMoreTokens()) {
			System.out.println(tokenizer.nextToken());
		}
	}

	public static void setClasspath(String classpath) {
		CLASSPATH = classpath;
	}

	protected static String getClasspath() {
		if (CLASSPATH == null) {
			setClasspath(System.getProperty("java.class.path"));
		}

		return CLASSPATH;
	}

	private static void instanceMain(String[] args) {

		if (args.length == 0) {
			printUsage();
		}

		for (int cmdIndex = 0; cmdIndex < args.length; cmdIndex++) {

			String cmd = args[cmdIndex];

			if ("-help".equals(cmd)) {
				printUsage();
			} else {
				which(cmd);
			}
		}
	}

	private static void printUsage() {

		System.out.println("\nSyntax: java JWhich [options] className");
		System.out.println("");
		System.out.println("where options include:");
		System.out.println("");
		System.out.println("\t-help     Prints usage information.");
		System.out.println("");
		System.out.println("Examples:");
		System.out.println("\tjava JWhich MyClass");
		System.out.println("\tjava JWhich my.package.MyClass");
		System.exit(0);
	}

	public static void main(String args[]) {
		JWhich.instanceMain(args);
	}
}
