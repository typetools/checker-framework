package org.checkerframework.checker.compilermsgs.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotation to distinguish compiler message Strings from
 * normal Strings. The programmer should hardly ever need to use it
 * explicitly.
 *
 * @author wmdietl
 * @checker_framework.manual #compilermsgs-checker Compiler Message Key Checker
 */
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface CompilerMessageKey {}
