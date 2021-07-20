package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * <p>You do not usually need to write {@code @KeyFor} on the key type in a map. That is, you can
 * declare variable {@code Map<String, Integer> myMap;} and the Nullness Checker will apply
 * {@code @KeyFor} as appropriate. If you redundantly write {@code @KeyFor}, as in {@code
 * Map<@KeyFor("myMap") String, Integer> myMap;}, then your code is more verbose, and more seriously
 * the Nullness Checker will issue errors when calling methods such as {@code Map.put}.
 *
 * @see EnsuresKeyFor
 * @see EnsuresKeyForIf
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownKeyFor.class)
public @interface KeyFor {
    /**
     * Java expression(s) that evaluate to a map for which the annotated type is a key.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    @JavaExpression
    public String[] value();
}
