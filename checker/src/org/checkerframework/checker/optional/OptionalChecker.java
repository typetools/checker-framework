package org.checkerframework.checker.optional;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker that prevents misuse of the {@link java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
// Uncomment when the Checker Framework assumes Java 8: @RelevantJavaTypes(Optional.class)
public class OptionalChecker extends BaseTypeChecker {}
