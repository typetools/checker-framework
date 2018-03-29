package org.checkerframework.checker.guieffect.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class declaration annotation to make methods default to {@code @AlwaysSafe}. While the normal
 * default is already {@code @AlwaysSafe} methods, this is useful for a type inside a package marked
 * {@code @UIPackage}.
 *
 * @checker_framework.manual #guieffect-checker GUI Effect Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SafeType {}
