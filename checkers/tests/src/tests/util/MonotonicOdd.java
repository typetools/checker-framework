package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

import checkers.quals.MonotonicQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

@TypeQualifier
@Inherited
@SubtypeOf(Unqualified.class)
@Target({ElementType.TYPE_USE})
@MonotonicQualifier(Odd.class)
public @interface MonotonicOdd {}
