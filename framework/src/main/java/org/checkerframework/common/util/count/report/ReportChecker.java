package org.checkerframework.common.util.count.report;

import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The Report Checker performs semantic searches over a program, for example, to find all methods
 * that override a specific method, all classes that inherit from a specific class, or all uses of
 * {@code do-while} loops (and not also {@code while} loops!).
 *
 * <p>The search is specified in two different ways.
 *
 * <p>The first way is to write a stub specification file using the annotations in {@code
 * org.checkerframework.common.util.count.report.qual.*}. You can see examples in the Checker
 * Framework repository at {@code framework/tests/report/reporttest.astub} and {@code
 * framework/jtreg/StubParserEnum/AnnotationFileParserEnum.astub}.
 *
 * <p>The second way is the {@code -AreportTreeKinds} and {@code -AreportModifiers} options, which
 * search for specific tree kinds or modifiers.
 *
 * <p>Some similar features are available from IDEs (e.g., show references), but this tool provides
 * much more flexibility and a command-line tool.
 *
 * <p>Options:
 *
 * <ul>
 *   <li>{@code -AreportTreeKinds}: comma-separated list of {@code Tree.Kind}s that should be
 *       reported
 *   <li>{@code -AreportModifiers}: comma-separated list of modifiers that should be reported
 * </ul>
 *
 * @see org.checkerframework.common.util.count.AnnotationStatistics
 * @see org.checkerframework.common.util.count.JavaCodeStatistics
 */
@SupportedOptions({"reportTreeKinds", "reportModifiers"})
public class ReportChecker extends BaseTypeChecker {

  /** Creates a ReportChecker. */
  public ReportChecker() {}
}
