package org.checkerframework.framework.testchecker.elementdefault;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.testchecker.util.SubQual;

/** The bottom of this test type system's hierarchy, below both siblings. */
@SubtypeOf({SubQual.class, ElementDefaultQual.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ElementDefaultBottom {}
