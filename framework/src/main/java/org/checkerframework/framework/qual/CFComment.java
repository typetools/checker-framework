package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is to be used to add comments related to
 * Checker-Framework. This annotation can be used on packages, classes, interfaces and
 * any elements inside these.
 * 
 * <p>Each comment should first start with the type-system to which comment is related to followed by ':' and then actual comment.
 * Provide single string if comment is for only one type-system and  multiple string for separate type-system.
 * 
 * <p>Here is an example:
 * 
 * <pre><code>
 * {@literal @}CFComment("type-system:some comment")
 * public class {
 *     {@literal @}CFComment({"nullness:Return type should be nullable","signedness:Comment related to Signedness type-system"})
 *     public String test{
 *         return "test";
 *     }
 * }
 * </code></pre>
 * 
 * <p>As a matter of style, programmers should always use this annotation
 * on the most deeply nested element where the comment related to Checker-Framework
 * is to be provided.
 * 
 * <p>These annotations are {@link Documented} and hence will appear in Javadocs.
 */
@Documented
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, PACKAGE})
@Retention(RetentionPolicy.SOURCE)
public @interface CFComment {
    /**
     * @return array of {@link String} containing comments
     */
    String[] value();
}