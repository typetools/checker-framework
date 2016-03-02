package org.checkerframework.checker.igj.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that the annotated reference is an immutable reference to an
 * immutable object.
 *
 * An Immutable object cannot be modified. Its fields may be reassigned or
 * mutated only if they are explicitly marked as Mutable or Assignable.
 *
 * @checker_framework.manual #igj-checker IGJ Checker
 */
@SubtypeOf(AssignsFields.class)
@DefaultQualifierInHierarchy
@DefaultFor({DefaultLocation.EXCEPTION_PARAMETER})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Mutable {}
