package org.checkerframework.checker.testchecker.disbaruse.qual;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface DisbarUseTop {}
