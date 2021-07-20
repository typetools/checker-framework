package org.checkerframework.framework.testchecker.testaccumulation.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Polymorphic qualifier for the test accumulation type system. */
@PolymorphicQualifier(TestAccumulation.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyTestAccumulation {}
