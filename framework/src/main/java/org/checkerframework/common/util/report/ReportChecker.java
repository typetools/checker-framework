package org.checkerframework.common.util.report;

import org.checkerframework.common.basetype.BaseTypeChecker;

import javax.annotation.processing.SupportedOptions;

/**
 * The Report Checker for semantic searches.
 *
 * <p>See the qualifiers for documentation.
 *
 * <p>Options: reportTreeKinds: comma-separated list of Tree.Kinds that should be reported.
 */
@SupportedOptions({"reportTreeKinds", "reportModifiers"})
public class ReportChecker extends BaseTypeChecker {}
