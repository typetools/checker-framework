package org.checkerframework.checker.testchecker.disbaruse.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** The Disbar Use Checker isses a warning when an expression of this type is used. */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER})
public @interface DisbarUse {}
