package org.checkerframework.checker.formatter;

import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker plug-in for the {@link Format} qualifier that finds
 * syntactically invalid formatter calls.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
public class FormatterChecker extends BaseTypeChecker {
}
