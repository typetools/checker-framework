package org.checkerframework.common.delegation.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation that indicates a field is a delegate, fields are not delegates by default.
 *
 * <p>Here is a way that this annotation may be used:
 *
 * <pre><code>
 * class MyEnumeration&lt;T&gt; implements Enumeration&lt;T&gt; {
 *    {@literal @}Delegate
 *    private Enumeration&lt;T&gt; e;
 *
 *    public boolean hasMoreElements() {
 *      return e.hasMoreElements();
 *    }
 * }
 * </code></pre>
 *
 * In the example above, {@code MyEnumeration.hasMoreElements()} delegates a call to {@code
 * e.hasMoreElements()}.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Delegate {}
