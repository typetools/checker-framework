package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * Syntactic sugar for both @PolyValue and @PolySameLen.
 *
 * @see org.checkerframework.common.value.qual.PolyValue
 * @see PolySameLen
 * @checker_framework.manual #index-checker Index Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@PolymorphicQualifier(UpperBoundUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyLength {}
