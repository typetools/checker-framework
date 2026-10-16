package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience alias meaning
 * {@code @PolyGrowable @PolySeqGrowable @PolyShrinkable @PolyReplaceable}. A polymorphic qualifier
 * for the four capability hierarchies.
 *
 * <p>This alias does not expand into the Iterator hierarchy. Write {@code @PolyIteratorPolyMod}
 * explicitly to make a method preserve {@code @IteratorPolyMod}.
 *
 * <p>Write {@code @PolyModifiable} on methods that preserve or transfer modifiability, such as
 * {@code List.subList()}.
 *
 * <p>For example:
 *
 * <pre><code>
 * interface Example&lt;E&gt; {
 * &nbsp; @PolyModifiable List&lt;E&gt; subList(@PolyModifiable Example&lt;E&gt; this, int from, int to);
 * }
 * </code></pre>
 *
 * At each call site, the return type is equal to the receiver type. If the receiver type is
 * {@code @Growable}, the return type is {@code @Growable}. If the receiver type is
 * {@code @Ungrowable}, the return type is {@code @Ungrowable}.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyModifiable {}
