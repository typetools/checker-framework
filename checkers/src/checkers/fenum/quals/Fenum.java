package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * @author wmdietl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface Fenum {
	String value() default "";
}