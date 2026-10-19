package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience alias that is syntactic sugar for {@code @}{@link MaybeModifiable}. It may only be
 * written within a formal parameter type.
 *
 * <p>You can write {@code @UnmodifiableParam} to indicate that a method does not change its
 * parameter. From the method's point of view, the parameter is unmodifiable. This is reference
 * unmodifiability, because another alias can change the value, unlike {@code @}{@link Unmodifiable}
 * which means object immutability: no alias can change the value.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnmodifiableParam {}
