package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom qualifier in the Unsigned Type
 * System.  It is only assigned to a value in error.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf({Constant.class})
@ImplicitFor(
    literals = {LiteralKind.NULL},
    types = {TypeKind.NULL}
)
public @interface SignednessBottom {}
