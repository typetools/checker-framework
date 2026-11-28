package org.checkerframework.checker.index.substringindex;

import java.util.Set;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * The Substring Index Checker is an internal checker that assists the Index Checker in typing the
 * results of calls to the JDK's {@link java.lang.String#indexOf(String) String.indexOf} and {@link
 * java.lang.String#lastIndexOf(String) String.lastIndexOf} routines.
 *
 * @checker_framework.manual #index-substringindex Index Checker
 */
@SuppressWarningsPrefix({"index", "substringindex"})
// int.class is for @SubstringIndexFor
@RelevantJavaTypes({CharSequence.class, Object[].class, int.class})
public class SubstringIndexChecker extends BaseTypeChecker {
  /** Creates a SubstringIndexChecker. */
  public SubstringIndexChecker() {}

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    // SubstringIndexVisitor uses the SameLen Checker's facts.
    checkers.add(SameLenChecker.class);
    return checkers;
  }
}
