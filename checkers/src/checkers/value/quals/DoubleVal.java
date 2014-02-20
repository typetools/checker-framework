package checkers.value.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * TODO Double or double value
 * 
 */
@TypeQualifier
@SubtypeOf({ UnknownVal.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface DoubleVal {
    double[] value();
}