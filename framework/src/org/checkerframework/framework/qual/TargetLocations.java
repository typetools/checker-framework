package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that restricts the type-use locations where a type
 * qualifier may be written.  When written together with
 * {@code @Target({ElementType.TYPE_USE})}, the given type qualifier may be
 * written only at locations listed in the {@code @TargetLocations(...)}
 * meta-annotation.  The absence of {@code @TargetLocations(...)}
 * means that the qualifier can be written on any type use.
 * <p>
 *
 * This enables a type system designer to permit a qualifier to be written
 * only in certain locations.  For example, some type systems' top and bottom
 * qualifier (such as
 * {@link org.checkerframework.checker.nullness.qual.KeyForBottom})
 * should only be written on an explicit wildcard upper or lower bound.
 * This meta-annotation is a declarative, coarse-grained approach to enable
 * that. A {@link org.checkerframework.common.basetype.TypeValidator} must be
 * implemented if a more fine-grained control is necessary.
 * <p>
 *
 * TODO: Verify this meta-annotation (step 3 in
 * <a href="https://github.com/typetools/checker-framework/issues/515">Issue #515</a>).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TargetLocations {
    /**
     * Type uses at which the qualifier is permitted to be written
     * in source code.
     */
    DefaultLocation[] value();
}
