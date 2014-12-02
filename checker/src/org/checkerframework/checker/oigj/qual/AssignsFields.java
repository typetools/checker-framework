package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 *
 * Indicates that the annotated method could assign (but not mutate) the fields
 * of {@code this} object.
 *
 * A method with an AssignsFields receiver may not use the receiver to
 * invoke other methods with mutable or immutable reciever.
 *
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
// TODO: Document this

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier // (for now)
@SubtypeOf(ReadOnly.class)
public @interface AssignsFields {}
