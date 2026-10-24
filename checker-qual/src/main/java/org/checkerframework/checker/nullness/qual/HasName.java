package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated path has at least one name element and is not the empty path.
 *
 * <p>For a {@link java.nio.file.Path}, this means that {@link java.nio.file.Path#getFileName()}
 * returns a non-null, non-empty path. For a {@link java.io.File}, this means that {@link
 * java.io.File#getName()} returns a non-empty string.
 *
 * <p>Roots (such as {@code /} and, on Windows, {@code C:\}, {@code C:}, and {@code
 * \\server\share\}) and the empty path {@code ""} are not {@code @HasName}. Every name element of a
 * {@code @HasName} path is non-empty, because only the empty path has an empty name element.
 *
 * <p>The property depends only on the characters of the path, not on the file system. Therefore,
 * once a path is known to be {@code @HasName}, it remains {@code @HasName}.
 *
 * @see HasParent
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownPathShape.class)
public @interface HasName {}
