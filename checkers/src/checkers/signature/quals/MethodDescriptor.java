package checkers.signature.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a method descriptor (JVM signature type for methods) as defined in Java Language Specification: http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html (sec 4.3.3) 
 * Example:
 *	package edu.cs.washington;
 *	public class BinaryName {
 *		private class Inner {
 *			public void method(Object obj, int i) {}
 *		}
 *	}
 * In this example method descriptor for method 'method': (Ljava/lang/Object;I)Z
 * @author Kivanc Muslu
 */
@TypeQualifier
@SubtypeOf({UnannotatedString.class})
public @interface MethodDescriptor {}
