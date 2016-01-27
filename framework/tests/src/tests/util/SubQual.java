package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;

/** A subtype of SuperQual. */
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultInUncheckedCodeFor({ DefaultLocation.PARAMETERS, DefaultLocation.LOWER_BOUNDS })
public @interface SubQual {}
