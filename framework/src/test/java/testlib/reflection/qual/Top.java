package testlib.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Toy type system for testing reflection resolution. Uses
 * org.checkerframework.common.subtyping.qual.Bottom as bottom.
 *
 * @see Sibling1, Sibling2
 */
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
public @interface Top {}
