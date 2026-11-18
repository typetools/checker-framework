package org.checkerframework.framework.testchecker.testaccumulation.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/** A test bottom type for an accumulation type system. */
@SubtypeOf({TestAccumulation.class, TestAccumulationPredicate.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface TestAccumulationBottom {}
