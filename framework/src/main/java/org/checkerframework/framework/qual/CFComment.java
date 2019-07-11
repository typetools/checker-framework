package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is for comments related to the Checker Framework.
 *
 * <p>Use of {@code @CFComment} makes it easy to find Checker-Framework-related comments, and to
 * copy those comments from one version of a codebase to another, using the Annotation File
 * Utilities.
 *
 * <p>Here is an example:
 *
 * <pre><code>
 * {@literal @}CFComment("interning: factory methods guarantee that all elements are interned")
 *  public class MyClass {
 *   {@literal @}CFComment({"nullness: non-null return type is more specific than in superclass",
 *                "signedness: comment related to Signedness type system"})
 *    public String myMethod(){ ... }
 * }
 * </code></pre>
 *
 * <p>As a matter of style, programmers should use this annotation on the most deeply nested element
 * to which the comment applies.
 *
 * <p>It is opitional to prefixing each comment by the name of the type system. Doing so makes the
 * comment is about makes the comments easier to understand and search for.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface CFComment {
    /** Comments about Checker Framework annotations. */
    String[] value();
}
