package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultForUncheckedCode;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/** A supertype of SubQual. */
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultForUncheckedCode({ DefaultLocation.RETURNS, DefaultLocation.UPPER_BOUNDS })
public @interface SuperQual {}
