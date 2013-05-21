package checkers.compilermsgs.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.propkey.quals.PropertyKey;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The annotation to distinguish compiler message Strings from
 * normal Strings. The programmer should hardly ever need to use it
 * explicitly.
 *
 * @author wmdietl
 */
@TypeQualifier
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface CompilerMessageKey {}
