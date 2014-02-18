package checkers.reflection.quals;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import java.lang.annotation.Target;

/**
 * Represents an object with an Unknown Class value, this could be a non-Class
 * value or the result of a method invocation that is not handled by this
 * checker or a Class variable that has not yet bee initialized to anything.
 * 
 * This annotation is the default in the hierarchy and may not be written in
 * source code.
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Target({})
@DefaultQualifierInHierarchy
public @interface UnknownClass {
}