package checkers.signature.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a field descriptor (JVM signature type) as defined in Java Language Specification: http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html (sec. 4.3.2)
 * Example:
 *	package edu.cs.washington;
 *	public class BinaryName {
 *		private class Inner {}
 *	}
 * In this example field descriptor for class BinaryName: Ledu/cs/washington/BinaryName;
 * and field descriptor for class Inner: Ledu/cs/washington/BinaryName$Inner;
 * @author Kivanc Muslu
 */
@TypeQualifier
@SubtypeOf({UnannotatedString.class})
public @interface FieldDescriptor {}