package org.checkerframework.framework.testchecker.variablenamedefault.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;

/** VariableNameDefault middle qualifier. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(VariableNameDefaultTop.class)
@DefaultFor(names = ".*middle.*", namesExceptions = ".*notmiddle.*")
public @interface VariableNameDefaultMiddle {}
