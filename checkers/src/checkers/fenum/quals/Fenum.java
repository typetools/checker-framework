package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A generic fake enumeration qualifier that is parameterized by a name.
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface Fenum {
    String value() default "";
}
