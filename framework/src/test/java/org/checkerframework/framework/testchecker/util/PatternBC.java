package org.checkerframework.framework.testchecker.util;

import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@SubtypeOf(PatternUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@QualifierForLiterals(stringPatterns = "^[BC]$")
public @interface PatternBC {}
