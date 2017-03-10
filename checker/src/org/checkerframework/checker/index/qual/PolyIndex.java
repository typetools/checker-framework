package org.checkerframework.checker.index.qual;

/**
 * A polymorphic qualifier for the Lower Bound and Upper Bound type systems.
 *
 * <p>Writing {@code @PolyIndex} is equivalent to writing {@link PolyUpperBound @PolyUpperBound}
 * {@link PolyLowerBound @PolyLowerBound}, and that is how it is treated internally by the checker.
 * Thus, if you write an {@code @PolyIndex} annotation, you might see warnings about
 * {@code @PolyUpperBound} or {@code @PolyLowerBound}.
 *
 * @checker_framework.manual #index-checker Index Checker
 * @see PolyUpperBound
 * @see PolyLowerBound
 */
public @interface PolyIndex {}
