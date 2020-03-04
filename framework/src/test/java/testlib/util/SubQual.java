package testlib.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/** A subtype of SuperQual. */
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SubQual {}
