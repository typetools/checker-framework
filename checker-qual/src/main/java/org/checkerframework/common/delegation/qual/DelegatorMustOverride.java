package org.checkerframework.common.delegation.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation that indicates a method that <i>must</i> be overridden in order for a
 * conditional postcondition to hold for a delegating class.
 *
 * <p>Here is a way that this annotation may be used:
 *
 * <p>Given a class that declares a method with a postcondition annotation:
 *
 * <pre><code>
 * class ArrayListVariant&lt;T&gt; {
 *    {@literal @}EnsuresPresentIf(result = true)
 *    {@literal @}DelegatorMustOverride
 *    public boolean hasMoreElements() {
 *      return e.hasMoreElements();
 *    }
 * }
 * </code></pre>
 *
 * A delegating client <i>must</i> override the method:
 *
 * <pre><code>
 * class MyArrayListVariant&lt;T&gt; extends ArrayListVariant&lt;T&gt; {
 *
 *    {@literal @}Delegate
 *     private ArrayListVariant&lt;T&gt; myList;
 *
 *
 *    {@literal @}Override
 *    {@literal @}EnsuresPresentIf(result = true)
 *    public boolean hasMoreElements() {
 *      return myList.hasMoreElements();
 *    }
 * }
 * </code></pre>
 *
 * Otherwise, a warning will be raised.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DelegatorMustOverride {}
