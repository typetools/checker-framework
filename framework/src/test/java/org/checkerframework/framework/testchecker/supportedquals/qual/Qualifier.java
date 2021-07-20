package org.checkerframework.framework.testchecker.supportedquals.qual;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@SubtypeOf({})
@Target(ElementType.TYPE_USE)
@DefaultQualifierInHierarchy
public @interface Qualifier {}
