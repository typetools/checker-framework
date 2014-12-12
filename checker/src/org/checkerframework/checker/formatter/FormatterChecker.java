package org.checkerframework.checker.formatter;

import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatBottom;
import org.checkerframework.checker.formatter.qual.InvalidFormat;
import org.checkerframework.checker.formatter.qual.UnknownFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
 * A type-checker plug-in for the {@link Format} qualifier that finds
 * syntactically invalid formatter calls.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@TypeQualifiers({ UnknownFormat.class, Format.class, FormatBottom.class, InvalidFormat.class })
public class FormatterChecker extends BaseTypeChecker {
}
