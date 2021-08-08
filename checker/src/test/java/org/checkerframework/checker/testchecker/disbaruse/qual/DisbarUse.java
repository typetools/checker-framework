package org.checkerframework.checker.testchecker.disbaruse.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER})
public @interface DisbarUse {}
