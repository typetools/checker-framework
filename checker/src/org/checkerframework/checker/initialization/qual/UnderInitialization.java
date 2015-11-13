package org.checkerframework.checker.initialization.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This type qualifier belongs to the freedom-before-commitment type-system for
 * tracking initialization. This type-system is not used on its own, but in
 * conjunction with some other type-system that wants to ensure safe
 * initialization. For instance, {@link NullnessChecker} uses
 * freedom-before-commitment to track initialization of {@link NonNull} fields.
 *
 * <p>
 * This type qualifier indicates that the object is (definitely) under
 * initialization at the moment and that no aliases that are typed differently
 * exist.
 *
 * <p>
 * Because the object is currently under initialization it is allowed to store
 * potentially not fully initialized object in fields.
 *
 * <p>
 * Similar to {@link UnknownInitialization}, this type qualifier supports type
 * frames.
 *
 * <p>
 * At the beginning of a constructor, the fields of the object are not yet
 * initialized and thus {@link UnknownInitialization UnknownInitialization(<em>supertype</em>)} is used
 * as the type of the self-reference {@code this}. Consider a class {@code B}
 * that is a subtype of {@code A}. At the beginning of the constructor of
 * {@code B}, {@code this} has the type {@code @UnderInitialization(A.class)},
 * since all fields of {@code A} have been initialized by the super-constructor.
 * If during the constructor also all fields of {@code B} are initialized, then
 * the type of {@code this} changes to {@code @UnderInitialization(B.class)} (and
 * otherwise, if not all fields are initialized, an error is issued).
 *
 * <p>
 * Note that it would not be sound to type {@code this} as {@link Initialized}
 * anywhere in a constructor (with the exception of final classes; but this is
 * currently not implemented), because there might be subclasses with
 * uninitialized fields. The following example shows why:
 *
 * <pre>
 * <code>
 *   class A {
 *      &#64;NonNull String a;
 *      public A() {
 *          a = "";
 *          // Now, all fields of A are initialized.
 *          // However, if this constructor is invoked as part of 'new B()', then
 *          // the fields of B are not yet initialized.
 *          // If we would type 'this' as &#64;Initialized, then the following call is valid:
 *          foo();
 *      }
 *      void foo() {}
 *   }
 *
 *   class B extends A {
 *      &#64;NonNull String b;
 *      &#64;Override
 *      void foo() {
 *          // Dereferencing 'b' is ok, since 'this' is &#64;Initialized and 'b' &#64;NonNull.
 *          // However, when executing 'new B()', this line throws a null-pointer exception.
 *          b.toString();
 *      }
 *   }
 * </code>
 * </pre>
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
@SubtypeOf(UnknownInitialization.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface UnderInitialization {
    /**
     * The type-frame down to which the expression (of this type) has been
     * initialized at least (inclusive). That is, an expression of type
     * {@code @UnderInitialization(T.class)} has all type-frames initialized
     * starting at {@code Object} down to (and including) {@code T}.
     */
    Class<?> value() default Object.class;
}
