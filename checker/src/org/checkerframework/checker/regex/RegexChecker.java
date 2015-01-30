package org.checkerframework.checker.regex;

import org.checkerframework.checker.regex.qual.PartialRegex;
import org.checkerframework.checker.regex.qual.PolyRegex;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.qual.RegexBottom;
import org.checkerframework.checker.regex.qual.UnknownRegex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@TypeQualifiers({ Regex.class, PartialRegex.class, RegexBottom.class,
    UnknownRegex.class, PolyRegex.class, PolyAll.class })
public class RegexChecker extends BaseTypeChecker {
}
