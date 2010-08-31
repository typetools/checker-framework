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
@Target( {} )
@TypeQualifier
@SubtypeOf( { } )
public @interface FenumTop {}
