package org.checkerframework.framework.testchecker.util;

import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

@Inherited
@SubtypeOf(Unqualified.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@MonotonicQualifier(Odd.class)
public @interface MonotonicOdd {}
