package org.checkerframework.common.delegation.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation that indicates a field is a delegate. Fields are not delegates by default.
 *
 * <p>A class is a <i>delegator</i> if it implements each method that overrides a supertype method
 * by calling the same method on some other object, which is called the <i>delegate</i>. (A
 * delegator can instead refuse to support an operation, by throwing {@code
 * UnsupportedOperationException}.) Delegation is an alternative to inheritance: rather than reusing
 * a superclass's implementation, the delegator forwards each call to an object that it wraps. A
 * class can have at most one field annotated with {@code @Delegate}.
 *
 * <p>Here is an example use of this annotation:
 *
 * <pre><code>
 * class MyEnumeration&lt;T&gt; implements Enumeration&lt;T&gt; {
 *   {@literal @}Delegate
 *   private Enumeration&lt;T&gt; e;
 *
 *   public boolean hasMoreElements() {
 *     return e.hasMoreElements();
 *   }
 *
 *   public T nextElement() {
 *     return e.nextElement();
 *   }
 * }
 * </code></pre>
 *
 * In the example above, {@code MyEnumeration.hasMoreElements()} delegates a call to {@code
 * e.hasMoreElements()}, and {@code MyEnumeration.nextElement()} delegates a call to {@code
 * e.nextElement()}.
 *
 * @checker_framework.manual #delegation-checker Delegation Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Delegate {}
