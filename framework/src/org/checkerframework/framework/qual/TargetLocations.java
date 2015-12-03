package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates locations where a particular qualifier
 * can be written.
 * The absence of {@link org.checkerframework.framework.qual.TargetLocations}
 * means that the qualifier can be written on any target location.
 *
 * This meta-annotation takes as argument a list of
 * {@link org.checkerframework.framework.qual.DefaultLocation} at which the
 * qualifier is permitted.
 *
 * The reason this is needed is that {@link ElementType} is too coarse.
 * An example is that many type systems' bottom qualifier
 * (such as {@link org.checkerframework.checker.nullness.qual.KeyForBottom})
 * should only be written on an explicit wildcard upper or lower bound.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TargetLocations {
    DefaultLocation[] value();
}