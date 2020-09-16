package org.checkerframework.framework.testchecker.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.SubtypeOf;

/** Denotes an exception that is particularly important. */
@SubtypeOf(Unqualified.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Critical {}
