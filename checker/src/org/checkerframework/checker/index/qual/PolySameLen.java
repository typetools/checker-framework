package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the SameLen type system.
 *
 * <p>* @checker_framework.manual #index-checker Index Checker
 */
@PolymorphicQualifier(SameLenUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolySameLen {}
