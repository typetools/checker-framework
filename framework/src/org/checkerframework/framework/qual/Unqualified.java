package org.checkerframework.framework.qual;

import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing an unqualified type in the qualifier
 * hierarchy, as an argument to {@link SubtypeOf#value()}, in the type qualifiers declarations.
 *
 * <p>This annotation may not be written in source code; it is an implementation detail of the
 * checker.
 *
 * <p>Note that because of the missing RetentionPolicy, the qualifier will not be stored in
 * bytecode.
 *
 * <p>Only use this qualifier when experimenting with very simple type systems. For any more
 * realistic type systems, introduce a top and bottom qualifier that gets stored in bytecode.
 */
@InvisibleQualifier
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface Unqualified {}
