/*
 * Copyright (c) 2005 Brian Goetz and Tim Peierls
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 *
 * Any republication or derived work distributed in source code form
 * must include this copyright and license notice.
 */
/*
 * Modified for use with the Checker Framework Lock Checker
 */

package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Indicates that a thread may dereference the value referred to by the annotated variable only if
 * the thread holds all the given lock expressions.
 *
 * <p>{@code @GuardedBy({})} is the default type qualifier.
 *
 * <p>The argument is a string or set of strings that indicates the expression(s) that must be held,
 * using the <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">syntax of
 * Java expressions</a> described in the manual. The expressions evaluate to an intrinsic (built-in,
 * synchronization) monitor or an explicit {@link java.util.concurrent.locks.Lock}. The expression
 * {@code "<self>"} is also permitted; the type {@code @GuardedBy("<self>") Object o} indicates that
 * the value referenced by {@code o} is guarded by the intrinsic (monitor) lock of the value
 * referenced by {@code o}.
 *
 * <p>Two {@code @GuardedBy} annotations with different argument expressions are unrelated by
 * subtyping.
 *
 * @see Holding
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #lock-examples-guardedby Example use of @GuardedBy
 */
@SubtypeOf(GuardedByUnknown.class)
@Documented
@DefaultQualifierInHierarchy
@DefaultFor({TypeUseLocation.EXCEPTION_PARAMETER, TypeUseLocation.UPPER_BOUND})
@DefaultInUncheckedCodeFor({TypeUseLocation.PARAMETER})
@ImplicitFor(
    types = {
        TypeKind.BOOLEAN,
        TypeKind.BYTE,
        TypeKind.CHAR,
        TypeKind.DOUBLE,
        TypeKind.FLOAT,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.SHORT
    },
    typeNames = {java.lang.String.class}
)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface GuardedBy {
    /**
     * The Java value expressions that need to be held.
     *
     * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
     *     Java expressions</a>
     */
    @JavaExpression
    String[] value() default {};
}
