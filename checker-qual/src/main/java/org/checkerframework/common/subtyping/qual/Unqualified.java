package org.checkerframework.common.subtyping.qual;

import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing an unqualified type in the qualifier
 * hierarchy, as an argument to {@link SubtypeOf#value()}, in a type qualifier declaration.
 *
 * <p>This annotation may not be written in source code; it is an implementation detail of the
 * checker.
 *
 * <p>Use this qualifier only when experimenting with very simple type systems. For any more
 * realistic type systems, introduce a top and bottom qualifier that gets stored in bytecode.
 *
 * @checker_framework.manual #subtyping-checker Subtyping Checker
 */
@Documented
@Retention(RetentionPolicy.SOURCE) // don't store in class file
@Target({}) // empty target prevents programmers from writing this in a program.
@InvisibleQualifier
@SubtypeOf({})
public @interface Unqualified {}
