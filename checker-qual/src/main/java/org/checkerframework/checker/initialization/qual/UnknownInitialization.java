package org.checkerframework.checker.initialization.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * This type qualifier indicates how much of an object has been fully initialized. An object is
 * fully initialized when each of its fields contains a value that satisfies the field's
 * declaration.
 *
 * <p>An expression of type {@code @UnknownInitialization(T.class)} refers to an object that has all
 * fields of {@code T} (and any super-classes) initialized. Just {@code @UnknownInitialization} is
 * equivalent to {@code @UnknownInitialization(Object.class)}.
 *
 * <p>A common use is
 *
 * <pre>{@code
 * void myMethod(@UnknownInitialization(MyClass.class) MyClass this, ...) { ... }
 * }</pre>
 *
 * which allows {@code myMethod} to be called from the {@code MyClass} constructor. See the manual
 * for more examples of how to use the annotation (the link appears below).
 *
 * <p>Reading a field of an object of type {@code @UnknownInitialization} might yield a value that
 * does not correspond to the declared type qualifier for that field. For instance, consider a
 * non-null field:
 *
 * <pre>@NonNull Object f;</pre>
 *
 * In a partially-initialized object, field {@code f} might be {@code null} despite its
 * {@literal @}{@link NonNull} type annotation.
 *
 * <p>What type qualifiers on the field are considered depends on the checker; for instance, the
 * {@link org.checkerframework.checker.nullness.NullnessChecker} considers {@link NonNull}. The
 * initialization type system is not used on its own, but in conjunction with some other type-system
 * that wants to ensure safe initialization.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultFor({TypeUseLocation.LOCAL_VARIABLE, TypeUseLocation.RESOURCE_VARIABLE})
public @interface UnknownInitialization {
  /**
   * The type-frame down to which the expression (of this type) has been initialized at least
   * (inclusive). That is, an expression of type {@code @UnknownInitialization(T.class)} has all
   * type-frames initialized starting at {@code Object} down to (and including) {@code T}.
   *
   * @return the type whose fields are fully initialized
   */
  Class<?> value() default Object.class;
}
