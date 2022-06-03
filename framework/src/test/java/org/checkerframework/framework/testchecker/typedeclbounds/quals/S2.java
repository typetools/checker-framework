package org.checkerframework.framework.testchecker.typedeclbounds.quals;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.UpperBoundFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Top.class})
@UpperBoundFor(typeKinds = {TypeKind.BOOLEAN})
@DefaultFor(typeKinds = {TypeKind.BOOLEAN})
public @interface S2 {}
