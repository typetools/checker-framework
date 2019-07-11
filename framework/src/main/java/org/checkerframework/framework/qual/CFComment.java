package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is to be used to add comments related to the Checker Framework.
 *
 * <p>Purpose of {@code @CFComment} annotation is:
 *
 * <ul>
 *   <li>To make it easy to find Checker-Framework-related comments in a large codebase.
 *   <li>To make it easy to copy those comments from one codebase to another, using the Annotation
 *       File Utilities.
 * </ul>
 *
 * <p>While using {@code @CFComment} it is recommended that each comment should first start with the
 * type-system to which comment is related to followed by ':' and then actual comment. A user could
 * also break a long comment across lines, by putting it in multiple strings.
 *
 * <p>Here is an example:
 *
 * <pre><code>
 * {@literal @}CFComment("type-system:some comment")
 * public class {
 *     {@literal @}CFComment({"nullness:Return type should be nullable",
 *      "signedness:Comment related to Signedness type-system"})
 *     public String test{
 *         return "test";
 *     }
 * }
 * </code></pre>
 *
 * <p>As a matter of style, programmers should always use this annotation on the most deeply nested
 * element where the comment related to Checker-Framework is to be provided.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface CFComment {
    String[] value();
}
