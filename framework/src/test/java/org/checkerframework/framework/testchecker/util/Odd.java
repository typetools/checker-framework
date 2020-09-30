package org.checkerframework.framework.testchecker.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf(MonotonicOdd.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Odd {}
