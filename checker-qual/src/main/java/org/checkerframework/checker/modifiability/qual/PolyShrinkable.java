package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Shrink hierarchy that preserves the shrink capability.
 *
 * <p>Use on methods that preserve shrinkability &mdash; for example, {@code Map.keySet()}.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(MaybeShrinkable.class)
public @interface PolyShrinkable {}
