package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultForUnannotatedCode;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/** A subtype of SuperQual. */
@TypeQualifier
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultForUnannotatedCode({ DefaultLocation.PARAMETERS, DefaultLocation.LOWER_BOUNDS })
public @interface SubQual {}
