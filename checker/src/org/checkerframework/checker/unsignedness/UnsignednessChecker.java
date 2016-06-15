package org.checkerframework.checker.unsignedness;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker that prevents mixing of unsigned and signed values,
 * and prevents meaningless operations on unsigned values.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
public class UnsignednessChecker extends BaseTypeChecker { }
