package org.checkerframework.checker.optional;

import java.util.Optional;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker that prevents misuse of the {@link java.util.Optional} class.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
// TODO: If the Nullness Checker is running, then for a call to ofNullable, if the argument has type
// @NonNull, make the return type have type @Present.
@RelevantJavaTypes(Optional.class)
@StubFiles({"javaparser.astub"})
public class OptionalChecker extends BaseTypeChecker {}
