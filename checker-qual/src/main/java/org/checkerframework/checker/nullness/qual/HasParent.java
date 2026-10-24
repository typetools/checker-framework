package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated path has a parent, and it has at least one name element.
 *
 * <p>For a {@link java.nio.file.Path}, this means that {@link java.nio.file.Path#getParent()}
 * returns a non-null value. For a {@link java.io.File}, this means that {@link
 * java.io.File#getParent()} and {@link java.io.File#getParentFile()} return a non-null value.
 *
 * <p>A path with a parent has either 2 or more name elements, or a root plus a name element. For
 * example, {@code a/b} and {@code /a} are {@code @HasParent}, but {@code a} and {@code /} are not.
 *
 * <p>{@code @HasParent} is a subtype of {@link HasName}.
 *
 * <p>The property depends only on the characters of the path, not on the file system. Therefore,
 * once a path is known to be {@code @HasParent}, it remains {@code @HasParent}.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(HasName.class)
public @interface HasParent {}
