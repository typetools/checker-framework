package checkers.signature.quals;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Represents a {@link BinaryName binary name} as defined in the <a
 * href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-13.html#jls-13.1">Java
 * Language Specification, section 13.1</a>, but only for a non-array type.
 * <p>
 * Not to be used by the programmer, only used internally.
 */
@TypeQualifier
@SubtypeOf({BinaryName.class, ClassGetName.class})
@ImplicitFor(stringPatterns="^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_][A-Za-z_0-9]*)?$")
// A @Target meta-annotation with an empty argument would prevent programmers
// from writing this in a program, but it might sometimes be useful.
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface BinaryNameForNonArray {}
