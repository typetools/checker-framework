package checkers.formatter.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this annotation is attached to a
 * {@link java.util.Formatter#format(String, Object...) Formatter.format} like
 * method, the framework checks that that the parameters passed as var-args are
 * compatible with the format string.
 * 
 * @author Konstantin Weitz
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FormatMethod {}
