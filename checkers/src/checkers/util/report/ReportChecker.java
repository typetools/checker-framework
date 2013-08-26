package checkers.util.report;

import javax.annotation.processing.SupportedOptions;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * The Report Checker for semantic searches.
 * 
 * See the qualifiers for documentation.
 *
 * Options:
 * reportTreeKinds: comma separated list of Tree.Kinds that should be reported.
 *
 */
@TypeQualifiers({ Unqualified.class })
@SupportedOptions({"reportTreeKinds", "reportModifiers"})
public class ReportChecker extends BaseTypeChecker {}
