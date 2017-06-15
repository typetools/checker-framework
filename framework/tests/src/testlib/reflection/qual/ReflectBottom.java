package testlib.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Toy type system for testing reflection resolution. Uses
 * org.checkerframework.framework.qual.Bottom as bottom
 *
 * @see Sibling1, Sibling2
 */
@SubtypeOf({Sibling1.class, Sibling2.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface ReflectBottom {}
