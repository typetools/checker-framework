package org.checkerframework.checker.iteration.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/** Either a non-iterator, or an iterator that might or might not have a next element. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
@DefaultFor(value = TypeUseLocation.LOWER_BOUND, types = Void.class)
@QualifierForLiterals(LiteralKind.NULL)
public @interface UnknownHasNext {}
