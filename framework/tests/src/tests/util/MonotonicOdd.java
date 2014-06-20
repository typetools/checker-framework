package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;
import org.checkerframework.framework.qual.Unqualified;

@TypeQualifier
@Inherited
@SubtypeOf(Unqualified.class)
@Target({ElementType.TYPE_USE})
@MonotonicQualifier(Odd.class)
public @interface MonotonicOdd {}
