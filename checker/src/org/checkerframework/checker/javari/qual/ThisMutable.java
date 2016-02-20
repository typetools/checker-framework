package org.checkerframework.checker.javari.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation used to represent a place holder immutability type, that is
 * equivalent to the ThisMutable type in the Javari typesystem.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #javari-checker Javari Checker
 */
@Target({}) // empty target prevents programmers from writing this in a program
@SubtypeOf(ReadOnly.class)
@DefaultFor(TypeUseLocation.FIELD)
public @interface ThisMutable {}
