package org.checkerframework.common.aliasing.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes that the object is used up and can no longer be used for any operation or assignment.
 *
 * @checker_framework.manual #linear-checker Linear Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@SubtypeOf({Linear.class})
public @interface Unusable {}
