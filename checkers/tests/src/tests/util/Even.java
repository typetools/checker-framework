package tests.util;

import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

@TypeQualifier
@Inherited
@SubtypeOf(Unqualified.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Even {}
