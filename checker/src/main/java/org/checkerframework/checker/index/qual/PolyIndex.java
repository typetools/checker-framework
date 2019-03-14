package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Lower Bound and Upper Bound type systems.
 *
 * <p>Writing {@code @PolyIndex} is equivalent to writing {@link PolyUpperBound @PolyUpperBound}
 * {@link PolyLowerBound @PolyLowerBound}, and that is how it is treated internally by the checker.
 * Thus, if you write an {@code @PolyIndex} annotation, you might see warnings about
 * {@code @PolyUpperBound} or {@code @PolyLowerBound}.
 *
 * @checker_framework.manual #index-checker Index Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 * @see PolyUpperBound
 * @see PolyLowerBound
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@PolymorphicQualifier(UpperBoundUnknown.class)
public @interface PolyIndex {}
