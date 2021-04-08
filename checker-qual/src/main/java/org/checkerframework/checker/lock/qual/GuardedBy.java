package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.UpperBoundFor;

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
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(GuardedByUnknown.class)
@DefaultQualifierInHierarchy
// These are required because the default for local variables is @GuardedByUnknown, but if the local
// variable is one of these type kinds, the default should be @GuardedByUnknown.
@DefaultFor(
    value = {TypeUseLocation.EXCEPTION_PARAMETER, TypeUseLocation.UPPER_BOUND},
    typeKinds = {
      TypeKind.BOOLEAN,
      TypeKind.BYTE,
      TypeKind.CHAR,
      TypeKind.DOUBLE,
      TypeKind.FLOAT,
      TypeKind.INT,
      TypeKind.LONG,
      TypeKind.SHORT
    },
    types = {String.class, Void.class})
@UpperBoundFor(
    typeKinds = {
      TypeKind.BOOLEAN,
      TypeKind.BYTE,
      TypeKind.CHAR,
      TypeKind.DOUBLE,
      TypeKind.FLOAT,
      TypeKind.INT,
      TypeKind.LONG,
      TypeKind.SHORT
    },
    types = String.class)
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
