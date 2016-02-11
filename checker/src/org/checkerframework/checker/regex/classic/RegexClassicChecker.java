package org.checkerframework.checker.regex.classic;

import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@StubFiles("apache-xerces.astub")
public class RegexClassicChecker extends BaseTypeChecker {
}
