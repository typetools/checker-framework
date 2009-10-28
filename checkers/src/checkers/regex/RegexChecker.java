package checkers.regex;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker plug-in for the {@link ValidRegex} qualifier that finds
 * syntactically invalid regular expressions.
 */
@TypeQualifiers({ ValidRegex.class, Unqualified.class })
public class RegexChecker extends BaseTypeChecker { }
