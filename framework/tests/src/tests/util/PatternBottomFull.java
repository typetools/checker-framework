package tests.util;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@SubtypeOf({PatternA.class, PatternB.class, PatternC.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(literals = { LiteralKind.NULL })
public @interface PatternBottomFull {}
