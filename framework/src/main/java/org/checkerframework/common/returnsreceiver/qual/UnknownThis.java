package org.checkerframework.common.returnsreceiver.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/**
 * The top type for the Returns Receiver Checker's type system. Values of the annotated type might
 * be the receiver ({@code this}) or might not. Programmers should rarely write this type.
 *
 * @checker_framework.manual #returns-receiver-checker Returns Receiver Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@SubtypeOf({})
@QualifierForLiterals(LiteralKind.NULL)
@DefaultFor(value = TypeUseLocation.LOWER_BOUND)
public @interface UnknownThis {}
