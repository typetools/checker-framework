package testlib.wholeprograminference.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Toy type system for testing field inference.
 *
 * @see Sibling1, Sibling2, Parent
 */
@SubtypeOf({ImplicitAnno.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface WholeProgramInferenceBottom {}
