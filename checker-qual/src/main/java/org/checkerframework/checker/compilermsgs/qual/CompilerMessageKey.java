package org.checkerframework.checker.compilermsgs.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A string that is definitely a compiler message key.
 *
 * @checker_framework.manual #compilermsgs-checker Compiler Message Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownCompilerMessageKey.class)
public @interface CompilerMessageKey {}
