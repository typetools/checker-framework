package org.checkerframework.checker.signedness;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * A type-checker that prevents mixing of unsigned and signed values, and prevents meaningless
 * operations on unsigned values.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@RelevantJavaTypes({Number.class, Character.class})
public class SignednessChecker extends BaseTypeChecker {}
