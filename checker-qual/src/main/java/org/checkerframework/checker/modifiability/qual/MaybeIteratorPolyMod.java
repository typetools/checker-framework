package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier in the Iterator hierarchy. The iterator of the annotated collection is not
 * known to preserve the collection's capabilities, so the result of {@code iterator()} and {@code
 * listIterator()} does not acquire a capability from the collection: it is the top qualifier in
 * every capability hierarchy -- {@code @MaybeShrinkable}, and for a {@code ListIterator} also
 * {@code @MaybeGrowable} and {@code @MaybeReplaceable}.
 *
 * <p>The iterator does inherit the collection's <em>lack</em> of a capability, since a collection
 * that cannot be modified has no iterator that can modify it. For example, an
 * {@code @Unshrinkable @MaybeIteratorPolyMod} collection has an {@code @Unshrinkable} iterator.
 *
 * <p>This is the default qualifier for unannotated types.
 *
 * @see IteratorPolyMod
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface MaybeIteratorPolyMod {}
