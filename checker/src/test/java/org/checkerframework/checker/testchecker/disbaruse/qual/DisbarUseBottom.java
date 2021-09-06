package org.checkerframework.checker.testchecker.disbaruse.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@DefaultFor(TypeUseLocation.LOWER_BOUND)
@SubtypeOf(DisbarUseTop.class)
@Target({ElementType.TYPE_USE})
public @interface DisbarUseBottom {}
