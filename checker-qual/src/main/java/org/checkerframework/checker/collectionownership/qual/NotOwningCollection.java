package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @NotOwningCollection} is a reference to a resource collection/array
 * that might not own the underlying collection/array. Therefore, it can perform only certain safe
 * operations: it can insert only elements that are definitely not owning, and it cannot discard or
 * overwrite elements (though it may remove and return an element, as {@code Queue.poll()} does).
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface NotOwningCollection {}
