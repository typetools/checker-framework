package checkers.signature.quals;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a method descriptor (JVM representation of method signature)
 * as defined in the <a
 * href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#7035">Java Virtual Machine Specification, section 4.3.3</a>.
 * <p>
 * Example:
 * <pre>
 *  package edu.cs.washington;
 *  public class BinaryName {
 *    private class Inner {
 *      public void method(Object obj, int i) {}
 *    }
 *  }
 * </pre>
 * In this example method descriptor for method 'method': (Ljava/lang/Object;I)Z
 */
@TypeQualifier
@SubtypeOf(UnannotatedString.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MethodDescriptor {}
