package checkers.signature.quals;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a binary name as defined in Java Language Specification: http://java.sun.com/docs/books/jls/second_edition/html/binaryComp.doc.html (sec 13.1)
 * Binary names for objects are created as the following:
 *	<package name>.<class name>$<inner class name>
 *	where
 *	<package name> is a fully qualified (dot seperated) name.
 * Example:
 *	package edu.cs.washington;
 *	public class BinaryName {
 *		private class Inner {}
 *	}
 * In this example binary name for class BinaryName: edu.cs.washington.BinaryName
 * and binary name for class Inner: edu.cs.washington.BinaryName$Inner
 * Notice that binary names and fully qualified names are same for top level classes and only differ
 * by a '$' vs. '.' for inner classes.
 * @author Kivanc Muslu
 */
@TypeQualifier
@SubtypeOf({UnannotatedString.class})
@ImplicitFor(stringPatterns="^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?(\\[\\])*$")
public @interface BinaryName {}
