package checkers.formatter;

import checkers.basetype.BaseTypeChecker;
import checkers.formatter.quals.Format;
import checkers.formatter.quals.FormatBottom;
import checkers.formatter.quals.InvalidFormat;
import checkers.formatter.quals.UnknownFormat;
import checkers.quals.TypeQualifiers;

/**
 * A type-checker plug-in for the {@link Format} qualifier that finds
 * syntactically invalid formatter calls.
 *
 * @checker_framework_manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@TypeQualifiers({ UnknownFormat.class, Format.class, FormatBottom.class, InvalidFormat.class })
public class FormatterChecker extends BaseTypeChecker {
}
