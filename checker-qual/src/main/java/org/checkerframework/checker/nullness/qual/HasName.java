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
 * <p>Roots (such as {@code /} and, on Windows, {@code C:\} and {@code C:}) and the empty path
 * {@code ""} are not {@code @HasName}. On Windows, a UNC path such as {@code \\server\share\} is a
 * root as a {@code Path}, so it is not {@code @HasName}, but as a {@code File} its name is {@code
 * share}. Every name element of a {@code @HasName} path is non-empty, because only the empty path
 * has an empty name element.
 *
 * <p>Whether a path has a name depends only on the path's characters and on the rules by which the
 * platform (and, for a {@code File}, the JDK version) parses them; for example, {@code C:} has a
 * name on Unix but not on Windows. It does not depend on the state of the file system; for example,
 * it does not depend on whether the file exists. Therefore, no operation, such as creating or
 * deleting a file, can make a {@code @HasName} path lose its name.
 *
 * @see HasParent
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownPathShape.class)
public @interface HasName {}
