package checkers.reflection.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This represents a Class<T> object where the exact value of <T> is known. This
 * is a list of values, so that if the object could have multiple different
 * exact values each is represented
 */
@TypeQualifier
@SubtypeOf({ UnknownClass.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE })
public @interface ClassVal {
    String[] value();
}