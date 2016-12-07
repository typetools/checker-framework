package testlib.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({PatternAB.class, PatternAC.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(stringPatterns = "^[A]$")
public @interface PatternA {}
