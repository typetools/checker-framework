package org.checkerframework.checker.nonempty;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker that prevents {@link java.util.NoSuchElementException} in the use of container
 * classes.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
public class NonEmptyChecker extends BaseTypeChecker {}
