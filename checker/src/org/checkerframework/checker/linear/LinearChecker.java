package org.checkerframework.checker.linear;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker plug-in for the Linear type system. A {@code @Linear} reference may be used only
 * one time. After that, it is "used up" and of type {@code @Unusable}, and any further use is a
 * compile-time error.
 *
 * @checker_framework.manual #linear-checker Linear Checker
 */
public class LinearChecker extends BaseTypeChecker {}
