package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * Used internally by the type system; should never be written by a programmer.
 *
 * <p>
 * Indicates that the value assigned to the annotated variable is not known to be
 * a key for any map.  It is the top type qualifier in the
 * {@link KeyFor} hierarchy.  It is also the default type qualifier.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnknownKeyFor {}
