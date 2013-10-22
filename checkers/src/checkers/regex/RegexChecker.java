package checkers.regex;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.regex.quals.PartialRegex;
import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import checkers.regex.quals.RegexBottom;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds
 * syntactically invalid regular expressions.
 *
 * @checker.framework.manual #regex-checker Regex Checker
 */
@TypeQualifiers({ Regex.class, PartialRegex.class, RegexBottom.class,
    Unqualified.class, PolyRegex.class, PolyAll.class })
public class RegexChecker extends BaseTypeChecker {
}
