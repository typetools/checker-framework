package org.checkerframework.checker.compilermsgs.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom qualifier for the Compiler Message Key Checker.
 *
 * @checker_framework.manual #compilermsgs-checker Compiler Message Key Checker
 */
@SubtypeOf(CompilerMessageKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(
    typeNames = {java.lang.Void.class},
    literals = {LiteralKind.NULL}
)
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface CompilerMessageKeyBottom {}
