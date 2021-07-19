package org.checkerframework.framework.testchecker.nontopdefault.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultFor({
    TypeUseLocation.LOCAL_VARIABLE,
    TypeUseLocation.IMPLICIT_UPPER_BOUND,
    TypeUseLocation.RECEIVER
})
public @interface NTDTop {}
