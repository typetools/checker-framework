package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Indicates that the annotated reference is a ReadOnly reference.
 * <p>
 *
 * A {@code ReadOnly} reference could refer to a Mutable or an Immutable
 * object. An object may not be mutated through a read only reference,
 * except if the field is marked {@code Assignable}. Only a method with a
 * readonly receiver can be called using a readonly reference.
 *
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf({})
public @interface ReadOnly {}
