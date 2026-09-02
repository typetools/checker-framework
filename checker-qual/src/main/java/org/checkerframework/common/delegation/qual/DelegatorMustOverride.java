package org.checkerframework.common.delegation.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates a method that <i>must</i> be overridden in order for a delegator's
 * specification to hold.
 *
 * <p>The Delegation Checker does not require a delegator to override every method of its
 * supertypes, because for many methods the inherited implementation is correct for the delegator as
 * well. Write {@code @DelegatorMustOverride} on a method whose specification &mdash; such as a
 * precondition, a postcondition, or a conditional postcondition &mdash; would not be satisfied by
 * an inherited implementation, and therefore requires a delegating implementation.
 *
 * <p>Here is a way that this annotation may be used:
 *
 * <p>Given a class that declares a method with a conditional postcondition:
 *
 * <pre><code>
 * class Sequence&lt;T&gt; {
 *
 *   private final List&lt;T&gt; elements;
 *
 *   {@literal @}DelegatorMustOverride
 *   {@literal @}EnsuresNonNullIf(expression = "peek()", result = true)
 *   public boolean hasNext() {
 *     return !elements.isEmpty();
 *   }
 *
 *   public {@literal @}Nullable T peek() {
 *     return elements.isEmpty() ? null : elements.get(0);
 *   }
 * }
 * </code></pre>
 *
 * A delegator <i>must</i> override the method, because the inherited implementation would consult
 * the delegator's own {@code elements} field rather than the delegate's:
 *
 * <pre><code>
 * class MySequence&lt;T&gt; extends Sequence&lt;T&gt; {
 *
 *   {@literal @}Delegate private final Sequence&lt;T&gt; delegate;
 *
 *   MySequence(Sequence&lt;T&gt; delegate) {
 *     this.delegate = delegate;
 *   }
 *
 *   {@literal @}Override
 *   {@literal @}EnsuresNonNullIf(expression = "peek()", result = true)
 *   public boolean hasNext() {
 *     return delegate.hasNext();
 *   }
 *
 *   {@literal @}Override
 *   public {@literal @}Nullable T peek() {
 *     return delegate.peek();
 *   }
 * }
 * </code></pre>
 *
 * If {@code MySequence} did not override {@code hasNext()}, the Delegation Checker would issue a
 * warning.
 *
 * @checker_framework.manual #delegation-checker Delegation Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DelegatorMustOverride {}
