package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This type represents any variable that isn't known to have the same length as another sequence.
 * This is the top type of the Same Length type system. Programmers should not need to write this
 * type.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SameLenUnknown {}
