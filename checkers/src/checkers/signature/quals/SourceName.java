package checkers.signature.quals;

import checkers.signature.quals.BinaryName;
import checkers.signature.quals.FullyQualifiedName;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import java.lang.annotation.Target;

/**
 * Since binary names (@BinaryName) and fully qualified names (@FullyQualifiedName) differ only
 * for inner classes (same for top level classes), checker framework cannot deduce if a fully
 * qualified name will be used as a fully qualified name or a binary name in the future. 
 * This is true since all fully qualified names are also valid binary names, where as the other
 * way around is not true.
 * Therefore source name (@SourceName) is an annotation that is only to be used by the checker
 * framework visitors when a fully qualified name (as String literal) is detected. This way 
 * that name is also permitted to be used a binary name (since it is valid).
 * Not to be used by the annotator, only used internally.
 * @author Kivanc Muslu
 */
@TypeQualifier
@SubtypeOf({BinaryName.class, FullyQualifiedName.class})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface SourceName {}
