package org.checkerframework.checker.iteration.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * An expression whose type has this annotation is an iterator that has a next value -- that is,
 * {@code next()} will not throw NoSuchElementException.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownHasNext.class})
public @interface HasNext {}
