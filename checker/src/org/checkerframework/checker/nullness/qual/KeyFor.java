package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that the value assigned to the annotated variable is a key for at least the given
 * map(s).
 *
 * <p>The value of the annotation is the reference name of the map. Suppose that {@code config} is a
 * {@code Map<String, String>}. Then the declaration
 *
 * <pre>{@code   @KeyFor("config") String key = "HOSTNAME"; }</pre>
 *
 * indicates that "HOSTNAME" is a key in {@code config}.
 *
 * <p>The value of the annotation can also be a set of reference names of the maps. If {@code
 * defaultConfig} is also a {@code Map<String, String>}, then
 *
 * <pre>{@code   @KeyFor({"config","defaultConfig"}) String key = "HOSTNAME"; }</pre>
 *
 * indicates that "HOSTNAME" is a key in {@code config} and in {@code defaultConfig}.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@SubtypeOf(UnknownKeyFor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface KeyFor {
    /**
     * Java expression(s) that evaluate to a map for which the annotated type is a key.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    @JavaExpression
    public String[] value();
}
