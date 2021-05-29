package org.checkerframework.checker.regex;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds syntactically invalid regular
 * expressions.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@StubFiles("apache-xerces.astub")
@RelevantJavaTypes({
  CharSequence.class,
  // javax.swing.text.Segment.class
  char.class,
  Character.class,
  Pattern.class,
  Matcher.class,
  MatchResult.class
})
public class RegexChecker extends BaseTypeChecker {}
