package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * Indicates that the value assigned to the annotated variable is a key for at least the given map(s).
 *
 * <p>
 * The value of the annotation is the reference name of the map.
 * Suppose that <tt>config</tt> is a <tt>Map&lt;String, String&gt;</tt>.
 * Then the declaration
 *
 * <pre><code>  @KeyFor("config") String key = "HOSTNAME"; </code></pre>
 *
 * indicates that "HOSTNAME" is a key in <tt>config</tt>.
 *
 * <p>
 * The value of the annotation can also be a set of reference names of the maps.
 * If <tt>defaultConfig</tt> is also a <tt>Map&lt;String, String&gt;</tt>, then
 * 
 * <pre><code>  @KeyFor({"config","defaultConfig"}) String key = "HOSTNAME"; </code></pre>
 * 
 * indicates that "HOSTNAME" is a key in <tt>config</tt> and in <tt>defaultConfig</tt>.
 * 
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@TypeQualifier
@SubtypeOf(UnknownKeyFor.class)
@Documented
@FieldIsExpression
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface KeyFor {
    /**
     * Java expression(s) that evaluate to a map for which the annotated type is a key.
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    public String[] value();
}
