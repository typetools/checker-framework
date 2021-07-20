package org.checkerframework.checker.optional;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

import java.util.Optional;

/**
 * A type-checker that prevents misuse of the {@link java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
// TODO: For a call to ofNullable, if the argument has type @NonNull, make the return type have type
// @Present.  Make Optional Checker a subchecker of the Nullness Checker.
@RelevantJavaTypes(Optional.class)
public class OptionalChecker extends BaseTypeChecker {}
