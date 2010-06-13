package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;


/**
 * 
 * @author wmdietl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@TypeQualifier
@SubtypeOf( { FenumTop.class } )
public @interface FenumDecl {
    String value() default "";
}