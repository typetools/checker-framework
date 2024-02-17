package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Ths type qualifier belongs to the Delegation type system. It is not used on its own, but in
 * conjunction with other type systems that need to reason about delegation in method calls, such as
 * {@link org.checkerframework.checker.nonempty.NonEmptyChecker}.
 *
 * <p>This type qualifier indicates that the object acts as a delegate through which calls are made;
 * postconditions that apply to the annotated object's method should also apply to the method that
 * delegates calls to the annotated object.
 *
 * <p>TODO: create manual entry for Delegation Checker and add here.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownDelegate.class)
public @interface Delegate {}
