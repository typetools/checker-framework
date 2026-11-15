package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated {@link java.nio.file.Path} or {@link java.io.File} might be a root, such as {@code
 * /}, or the empty path. It is the top type qualifier in the Path Shape type system. It is also the
 * default type qualifier.
 *
 * <p>Programmers rarely write this annotation.
 *
 * @see HasName
 * @see HasParent
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface UnknownPathShape {}
