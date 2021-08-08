package org.checkerframework.checker.testchecker.disbaruse.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

@Target({ElementType.TYPE_USE})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface DisbarUseTop {}
