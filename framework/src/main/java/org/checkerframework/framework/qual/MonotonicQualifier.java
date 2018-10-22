package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that a qualifier indicates that an expression goes monotonically
 * from a type qualifier {@code T} to another qualifier {@code S}. The annotation {@code S} is
 * called the <em>target</em> of the monotonic qualifier, and has to be indicated by {@link
 * MonotonicQualifier#value()}.
 *
 * <p>This meta-annotation can be used on the declaration of the monotonic qualifier used for the
 * type-system at hand, and is often called {@code MonoT} if the target is {@code T}. The subtyping
 * hierarchy has to be defined as follows:
 *
 * <pre>{@code
 * T <: MonoT <: S
 * }</pre>
 *
 * where {@code <:} indicates the subtyping relation.
 *
 * <p>An expression of a monotonic type can only be assigned expressions of the target type {@code
 * T}. This means that an expression of the monotonic type {@code MonoT} cannot be assigned to a
 * variable of the same type.
 *
 * <p>Reading an expression of a monotonic type {@code MonoT} might always yield an expression of
 * type {@code S}. However, once it has been observed that a variable has the target type {@code T},
 * the monotonic property ensures that it will stay of type {@code T} for the rest of the program
 * execution. This is even true if arbitrary other code is executed.
 *
 * <p>Note that variables of a monotonic type can be re-assigned arbitrarily often, but only with
 * expressions of the target type.
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MonotonicQualifier {
    Class<? extends Annotation> value();
}
