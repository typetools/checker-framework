package checkers.formatter;

import checkers.basetype.BaseTypeChecker;
import checkers.formatter.quals.Format;
import checkers.formatter.quals.FormatBottom;
import checkers.formatter.quals.InvalidFormat;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker plug-in for the {@link Format} qualifier that finds
 * syntactically invalid formatter calls.
 *
 * @checker.framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@TypeQualifiers({ Unqualified.class, Format.class, FormatBottom.class, InvalidFormat.class })
public class FormatterChecker extends BaseTypeChecker {
}
