package checkers.signature.quals;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.signature.quals.BinaryName;
import checkers.signature.quals.FullyQualifiedName;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * A source name is a string that is a valid {@linkplain FullyQualifiedName
 * fully qualified name} and a valid {@linkplain BinaryName binary name}.
 * <p>
 * Not to be used by the programmer, only used internally.
 */
@TypeQualifier
@SubtypeOf({BinaryName.class, FullyQualifiedName.class})
// A @Target meta-annotation with an empty argument would prevent programmers
// from writing this in a program, but it might sometimes be useful.
public @interface SourceName {}
