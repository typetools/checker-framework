package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NOTE: This meta-annotation is <a
 * href="https://github.com/typetools/checker-framework/issues/1919"><b>not currently
 * enforced</b></a>.
 *
 * <p>A meta-annotation that restricts the type-use locations where a type qualifier may be written.
 * When written together with {@code @Target({ElementType.TYPE_USE})}, the given type qualifier may
 * be written only at locations listed in the {@code @TargetLocations(...)} meta-annotation.
 * {@code @Target({ElementType.TYPE_USE})} together with no {@code @TargetLocations(...)} means that
 * the qualifier can be written on any type use.
 *
 * <p>This enables a type system designer to permit a qualifier to be written only in certain
 * locations. For example, some type systems' top and bottom qualifier (such as {@link
 * org.checkerframework.checker.nullness.qual.KeyForBottom}) should only be written on an explicit
 * wildcard upper or lower bound. This meta-annotation is a declarative, coarse-grained approach to
 * enable that. For finer-grained control, override {@code visit*} methods that visit trees in
 * BaseTypeVisitor.
 *
 * <p>This annotation does not prevent the type system from defaulting, inferring, or computing the
 * given type annotation at the given location. It only prevents users from writing an explicit
 * annotation at the given location.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TargetLocations {
    /** Type uses at which the qualifier is permitted to be written in source code. */
    TypeUseLocation[] value();
}
