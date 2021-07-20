package org.checkerframework.framework.testchecker.typedecldefault.quals;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** TypeDeclDefault bottom qualifier. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(TypeDeclDefaultMiddle.class)
@QualifierForLiterals(LiteralKind.STRING)
@DefaultQualifierInHierarchy
public @interface TypeDeclDefaultBottom {}
