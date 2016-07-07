package org.checkerframework.checker.lowerbound.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;
import javax.lang.model.type.TypeKind;

/**
 * The bottom qualifier in the Lower Bound
 * Checker's type system.  It is only assigned to a value in error.
 *
 * @checker_framework.manual #lowerbound-checker Lower Bound Checker
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf( { Positive.class } )
public @interface LowerBoundBottom { }
