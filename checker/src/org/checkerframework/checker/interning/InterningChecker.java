package org.checkerframework.checker.interning;

import javax.annotation.processing.SupportedOptions;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedLintOptions;

/**
 * A type-checker plug-in for the {@link Interned} qualifier that finds (and verifies the absence
 * of) equality-testing and interning errors.
 *
 * <p>The {@link Interned} annotation indicates that a variable refers to the canonical instance of
 * an object, meaning that it is safe to compare that object using the "==" operator. This plugin
 * warns whenever "==" is used in cases where one or both operands are not {@link Interned}.
 * Optionally, it suggests using "==" instead of ".equals" where possible.
 *
 * @checker_framework.manual #interning-checker Interning Checker
 */
@StubFiles({"com-sun.astub", "org-jcp.astub", "org-xml.astub", "sun.astub"})
@SupportedLintOptions({"dotequals"})
@SupportedOptions({"checkclass"})
public final class InterningChecker extends BaseTypeChecker {}
