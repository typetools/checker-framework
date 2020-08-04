package org.checkerframework.checker.builder.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This declaration annotation indicates that the method on which it is written returns exactly the
 * receiver object.
 *
 * <p>This annotation can only be written on a method declaration. It is inherited by all overrides
 * of that method.
 *
 * <p>This annotation has been replaced by {@link
 * org.checkerframework.common.returnsreceiver.qual.This}. It is retained only for
 * backwards-compatibility, including with Lombok's checkerframework = true lombok.config flag. It
 * should not be used in new code, because it is TRUSTED, NOT CHECKED.
 *
 * <p>This annotation could be marked as deprecated, but that causes extra warnings when processing
 * delombok'd code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ReturnsReceiver {}
