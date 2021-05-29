package org.checkerframework.framework.testchecker.supportedquals.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE, ElementType.FIELD})
public @interface NotQualifier {}
