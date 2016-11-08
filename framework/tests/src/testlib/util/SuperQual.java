package testlib.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/** A supertype of SubQual. */
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultInUncheckedCodeFor({TypeUseLocation.RETURN, TypeUseLocation.UPPER_BOUND})
public @interface SuperQual {}
