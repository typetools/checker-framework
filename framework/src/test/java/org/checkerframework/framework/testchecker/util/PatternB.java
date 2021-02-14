package org.checkerframework.framework.testchecker.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({PatternAB.class, PatternBC.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@QualifierForLiterals(stringPatterns = "^[B]$")
public @interface PatternB {}
