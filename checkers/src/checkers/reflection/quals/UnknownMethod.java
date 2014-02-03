package checkers.reflection.quals;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Target;

/**
 * 
 * Represents an object with an Unknown Method value, this could be a non-Method
 * object or the result of a method invocation that is not handled by this
 * checker (only handled getMethod() and getDeclaredMethod()) or a Method
 * variable that has not yet been initialized to anything.
 * 
 * This annotation is the default in the hierarchy and may not be written in
 * source code.
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Target({})
@DefaultQualifierInHierarchy
public @interface UnknownMethod {
}