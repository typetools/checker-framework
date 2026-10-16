package org.checkerframework.checker.testchecker.disbaruse.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultFor(TypeUseLocation.LOWER_BOUND)
@SubtypeOf(DisbarUseTop.class)
@Target({ElementType.TYPE_USE})
public @interface DisbarUseBottom {}
