package org.checkerframework.framework.testchecker.supportedquals.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({Qualifier.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface BottomQualifier {}
