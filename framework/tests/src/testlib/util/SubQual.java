package testlib.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/** A subtype of SuperQual. */
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultInUncheckedCodeFor({TypeUseLocation.PARAMETER, TypeUseLocation.LOWER_BOUND})
public @interface SubQual {}
