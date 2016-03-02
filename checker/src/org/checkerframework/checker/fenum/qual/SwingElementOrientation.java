package org.checkerframework.checker.fenum.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * @checker_framework.manual #fenum-checker Fake Enum Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
public @interface SwingElementOrientation {}
