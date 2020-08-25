package org.checkerframework.checker.builder.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A deprecated variant of {@code org.checkerframework.common.returnsreceiver.qual.This}.
 *
 * <p>Lombok outputs this annotation. It is retained only for backwards-compatibility with Lombok's
 * {@code checkerframework = true} lombok.config flag. It should not be used in new code, because it
 * is TRUSTED, NOT CHECKED.
 *
 * <p>This annotation could be marked as deprecated, but that causes extra warnings when processing
 * delombok'd code.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ReturnsReceiver {}
