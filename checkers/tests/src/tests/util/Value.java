package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

@TypeQualifier
@SubtypeOf(Unqualified.class)
@Target(ElementType.TYPE_USE)
public @interface Value {
    int value() default 0;
}
