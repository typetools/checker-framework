package checkers.signature.quals;

import checkers.signature.quals.BinaryName;
import checkers.signature.quals.FullyQualifiedName;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import java.lang.annotation.Target;

/**
 * A source name is a string that is a valid {@linkplain FullyQualifiedName
 * fully qualified name} and a valid {@linkplain BinaryName binary name}.
 * <p>
 * Not to be used by the programmer, only used internally.
 */
@TypeQualifier
@SubtypeOf({BinaryName.class, FullyQualifiedName.class})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface SourceName {}
