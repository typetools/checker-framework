package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the {@code @NonEmpty} type system.
 *
 * <p>Any method written using {@code @PolyNonEmpty} conceptually has two versions: one in which
 * every instance of {@code @PolyNonEmpty} has been replaced by {@code @}{@link UnknownNonEmpty} and
 * one in which every instance of {@code @PolyNonEmpty} has been replaced by {@code @}{@link
 * NonEmpty}, for every possible combination of Collection, Map, or Iterator arguments.
 *
 * @checker_framework.manual #nonempty NonEmpty Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@PolymorphicQualifier(UnknownNonEmpty.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyNonEmpty {}
