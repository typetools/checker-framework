package checkers.regex;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.regex.quals.Regex;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 */
@TypeQualifiers({ Regex.class, Unqualified.class })
public class RegexChecker extends BaseTypeChecker { }
