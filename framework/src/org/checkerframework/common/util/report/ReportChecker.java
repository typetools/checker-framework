package org.checkerframework.common.util.report;

import javax.annotation.processing.SupportedOptions;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.qual.Unqualified;

/**
 * The Report Checker for semantic searches.
 * <p>
 *
 * See the qualifiers for documentation.
 * <p>
 *
 * Options:
 * reportTreeKinds: comma-separated list of Tree.Kinds that should be reported.
 *
 */
@TypeQualifiers({ Unqualified.class })
@SupportedOptions({"reportTreeKinds", "reportModifiers"})
public class ReportChecker extends BaseTypeChecker {}
