package org.checkerframework.framework.testchecker.util;

import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Denotes an object with a representation that has been encrypted. */
@SubtypeOf(Unqualified.class)
@DefaultFor({TypeUseLocation.LOWER_BOUND})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Encrypted {}
