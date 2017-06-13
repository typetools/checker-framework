package org.checkerframework.checker.optional;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * A type-checker that prevents mixing of unsigned and signed values, and prevents meaningless
 * operations on unsigned values.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
@RelevantJavaTypes({Number.class, Character.class})
public class OptionalChecker extends BaseTypeChecker {}
