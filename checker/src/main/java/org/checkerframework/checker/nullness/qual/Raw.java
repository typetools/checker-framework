package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * This type qualifier belongs to the rawness type-system for tracking initialization. This
 * type-system is not used on its own, but in conjunction with some other type-system that wants to
 * ensure safe initialization. For instance, {@link
 * org.checkerframework.checker.nullness.NullnessRawnessChecker} uses rawness to track
 * initialization of {@link NonNull} fields.
 *
 * <p>This type qualifier indicates that the object might not have been fully initialized. An object
 * is fully initialized when each of its fields contains a value that satisfies its type qualifier.
 * What type qualifiers are considered depends on the checker; for instance, the {@link
 * org.checkerframework.checker.nullness.NullnessRawnessChecker} considers {@link NonNull}.
 *
 * <p>Therefore, reading a field of an object of type {@link Raw} might yield a value that does not
 * correspond to the declared type qualifier for that field. For instance, in the {@link
 * org.checkerframework.checker.nullness.NullnessRawnessChecker}, a field might be {@code null} even
 * if it has been annotated as {@link NonNull}.
 *
 * <p>More precisely, an expression of type {@code @Raw(T.class)} refers to an object that has all
 * fields of {@code T} (and any super-classes) initialized (e.g., to a non-null value in the {@link
 * org.checkerframework.checker.nullness.NullnessRawnessChecker}). Just {@code @Raw} is equivalent
 * to {@code @Raw(Object.class)}.
 *
 * <p>At the beginning of a constructor, the fields of the object are not yet initialized and thus
 * {@link Raw Raw(<em>supertype</em>)} is used as the type of the self-reference {@code this}.
 * Consider a class {@code B} that is a subtype of {@code A}. At the beginning of the constructor of
 * {@code B}, {@code this} has the type {@code @Raw(A.class)}, since all fields of {@code A} have
 * been initialized by the super-constructor. If during the constructor also all fields of {@code B}
 * are initialized, then the type of {@code this} changes to {@code @Raw(B.class)} (and otherwise,
 * if not all fields are initialized, an error is issued).
 *
 * <p>At the end of the constructor, the type is not fully initialized. Rather, it is {@code
 * Raw(<em>supertype</em>)}.
 *
 * <p>Note that it would not be sound to type {@code this} as {@link NonRaw} anywhere in a
 * constructor (with the exception of final classes; but this is currently not implemented), because
 * there might be subclasses with uninitialized fields. The following example shows why:
 *
 * <pre><code>
 * class A {
 *   {@literal @}NonNull String a;
 *    public A() {
 *        a = "";
 *        // Now, all fields of A are initialized.
 *        // However, if this constructor is invoked as part of 'new B()', then
 *        // the fields of B are not yet initialized.
 *        // If we would type 'this' as @NonRaw, then the following call is valid:
 *        foo();
 *    }
 *    void foo() {}
 * }
 *
 * class B extends A {
 *   {@literal @}NonNull String b;
 *   {@literal @}Override
 *    void foo() {
 *        // Dereferencing 'b' is ok, since 'this' is @NonRaw and 'b' @NonNull.
 *        // However, when executing 'new B()', this line throws a null-pointer exception.
 *        b.toString();
 *    }
 * }
 * </code></pre>
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SubtypeOf({})
@DefaultFor({TypeUseLocation.LOCAL_VARIABLE, TypeUseLocation.RESOURCE_VARIABLE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Raw {
    /**
     * The type-frame down to which the expression (of this type) has been initialized at least
     * (inclusive). That is, an expression of type {@code @Raw(T.class)} has all type-frames
     * initialized starting at {@code Object} down to (and including) {@code T}.
     */
    Class<?> value() default Object.class;
}
