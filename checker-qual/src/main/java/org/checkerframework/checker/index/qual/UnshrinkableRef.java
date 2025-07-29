package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @UnshrinkableRef} may not be used to remove elements, e.g., by
 * calling {@code remove()} or {@code clear()} on it. The collection might be shrunk by some other
 * reference that aliases the expression of type {@code @UnshrinkableRef}.
 *
 * @checker_framework.manual #growonly-checker Grow-only Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
@InvisibleQualifier
public @interface UnshrinkableRef {}
