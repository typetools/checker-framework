package org.checkerframework.common.delegation.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates a method whose implementation is correct for a delegator that inherits
 * it, so a delegator need not override it.
 *
 * <p>The Delegation Checker requires a delegator to override each concrete method that it would
 * otherwise inherit from a supertype, because an inherited implementation generally reads the
 * receiver's fields, which hold the delegator's own state rather than the delegate's. An inherited
 * implementation is correct for the delegator if its body uses the receiver only by calling
 * overridable methods on it: dynamic dispatch sends those calls to the delegator's overriding
 * implementations, which forward them to the delegate. Write {@code @InheritableByDelegator} on
 * such a method.
 *
 * <p>For example:
 *
 * <pre><code>
 * class Sequence&lt;T&gt; {
 *
 *   private final List&lt;T&gt; elements = new ArrayList&lt;&gt;();
 *
 *   public int size() {
 *     return elements.size();
 *   }
 *
 *   {@literal @}InheritableByDelegator
 *   public boolean isEmpty() {
 *     return size() == 0;
 *   }
 * }
 *
 * class MySequence&lt;T&gt; extends Sequence&lt;T&gt; {
 *
 *   {@literal @}Delegate private final Sequence&lt;T&gt; delegate;
 *
 *   MySequence(Sequence&lt;T&gt; delegate) {
 *     this.delegate = delegate;
 *   }
 *
 *   {@literal @}Override
 *   public int size() {
 *     return delegate.size();
 *   }
 * }
 * </code></pre>
 *
 * {@code MySequence} need not override {@code isEmpty()}: the inherited implementation calls {@code
 * size()}, which dispatches to {@code MySequence.size()} and thus to the delegate. If {@code
 * MySequence} did not override {@code size()}, the Delegation Checker would issue a warning,
 * because the inherited implementation of {@code size()} would read the delegator's own, empty,
 * {@code elements} field.
 *
 * <p>The Delegation Checker verifies each method that is annotated with {@code
 * InheritableByDelegator}: the method's body uses its receiver only as the receiver of calls to
 * methods that are overridable or are themselves annotated with {@code InheritableByDelegator}.
 *
 * <p>A delegator need not override a method that is abstract, final, or static, a default method of
 * an interface (which cannot read instance fields), or a method of {@code Object} (which has no
 * fields). Those methods need not be annotated with {@code InheritableByDelegator}.
 *
 * @checker_framework.manual #delegation-checker Delegation Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface InheritableByDelegator {}
