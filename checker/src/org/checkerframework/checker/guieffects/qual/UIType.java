package org.checkerframework.checker.guieffects.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class declaration annotation to make methods default to {@code @UI}.
 *
 * @checker_framework_manual #guieffects-checker GUI Effects Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface UIType {}
