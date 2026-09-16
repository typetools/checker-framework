package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If a collection's type is {@code @IteratorPolyMod}, then its {@code iterator()} and {@code
 * listIterator()} methods preserve the ability to call modification methods in its iterator.
 *
 * <p>For example, if collection {@code c} has type {@code @Shrinkable}, then {@code c.iterator()}
 * also has type {@code @Shrinkable}. If list {@code l} has type {@code @Replaceable}, then {@code
 * l.listIterator()} also has type {@code @Replaceable}.
 *
 * <p>If the collection itself is {@code @Unmodifiable}, then its iterator is {@code @Unshrinkable}.
 * That holds no matter what the collection's Iterator qualifier is: a {@code @MaybeIteratorPolyMod}
 * collection's iterator does not acquire the collection's capabilities, but it does inherit the
 * collection's lack of them. Only a collection that has neither the capability nor its negation --
 * for example, a {@code @MaybeShrinkable @MaybeIteratorPolyMod} collection -- has a
 * {@code @MaybeShrinkable} iterator.
 *
 * <p>The Grow, Shrink, and Replace Checkers consult this annotation; the SeqGrow Checker does not,
 * because an iterator has no sequenced-grow methods.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(MaybeIteratorPolyMod.class)
public @interface IteratorPolyMod {}
