package org.checkerframework.framework.testchecker.supportedquals.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({})
@Target(ElementType.TYPE_USE)
@DefaultQualifierInHierarchy
public @interface Qualifier {}
