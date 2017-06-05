package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Constant Value Checker.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@PolymorphicQualifier(UnknownVal.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyValue {}
