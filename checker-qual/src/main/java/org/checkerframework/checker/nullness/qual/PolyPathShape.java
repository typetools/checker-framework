package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Path Shape type system.
 *
 * <p>Any method written using {@code @PolyPathShape} conceptually has four versions: one in which
 * every instance of {@code @PolyPathShape} has been replaced by {@link UnknownPathShape}, one in
 * which every instance has been replaced by {@link HasName}, one in which every instance has been
 * replaced by {@link HasParent}, and one in which every instance has been replaced by {@link
 * PathShapeBottom}.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownPathShape.class)
public @interface PolyPathShape {}
