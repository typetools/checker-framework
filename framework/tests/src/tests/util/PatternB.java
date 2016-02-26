package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

import org.checkerframework.framework.qual.ImplicitFor;
@SubtypeOf({PatternAB.class, PatternBC.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(stringPatterns="^[B]$")
public @interface PatternB {}
