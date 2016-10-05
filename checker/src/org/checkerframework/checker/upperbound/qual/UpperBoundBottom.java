package org.checkerframework.checker.upperbound.qual;

import java.lang.annotation.*;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.framework.qual.*;

/**
 *  A bottom type for the Upper Bound type system.
 *
 *  */
@SubtypeOf(LessThanLength.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UpperBoundBottom {}
