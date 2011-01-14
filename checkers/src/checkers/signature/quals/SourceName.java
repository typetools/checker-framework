package checkers.signature.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@TypeQualifier
@SubtypeOf({BinaryName.class, FullyQualifiedName.class})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface SourceName {}
