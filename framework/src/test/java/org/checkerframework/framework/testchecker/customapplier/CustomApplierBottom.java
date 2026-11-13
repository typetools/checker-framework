package org.checkerframework.framework.testchecker.customapplier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.testchecker.util.SubQual;

/** The bottom qualifier of this test type system: a subtype of SubQual. */
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SubQual.class)
public @interface CustomApplierBottom {}
