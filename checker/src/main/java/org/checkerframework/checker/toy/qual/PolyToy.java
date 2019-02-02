package org.checkerframework.checker.toy.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/** A polymorphic qualifier for the Toy type system. */
@Documented
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyToy {}
