package org.checkerframework.common.util.report;

import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The Report Checker for semantic searches.
 *
 * <p>The Report Checker provides mechanisms to implement semantic searches over a program, for
 * example, to find all methods that override a specific method, all classes that inherit from a
 * specific class, or all uses of do-while-loops (and not also while loops!).
 *
 * <p>The search is specified by writing a stub specification file using the annotations in <code>
 * org.checkerframework.common.util.report.qual.*</code>.
 *
 * <p>Additionally, the <code>reportTreeKinds</code> option can be used to search for specific tree
 * kinds.
 *
 * <p>Some similar features are available from IDEs (e.g. show references), but this tool provides
 * much more flexibility and a command-line tool.
 *
 * <p>Options:
 *
 * <ul>
 *   <li><code>reportTreeKinds</code>: comma-separated list of <code>Tree.Kind</code>s that should
 *       be reported
 *   <li><code>reportModifiers</code>: comma-separated list of modifiers that should be reported
 * </ul>
 */
@SupportedOptions({"reportTreeKinds", "reportModifiers"})
public class ReportChecker extends BaseTypeChecker {}
