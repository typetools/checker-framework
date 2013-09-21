package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@TypeQualifier
@Inherited
@SubtypeOf(MonotonicOdd.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Odd {}
