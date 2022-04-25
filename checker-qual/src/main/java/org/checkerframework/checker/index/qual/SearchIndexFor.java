package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose length is between {@code -a.length - 1}
 * and {@code a.length - 1}, inclusive, for all sequences {@code a} listed in the annotation.
 *
 * <p>This is the return type of {@link java.util.Arrays#binarySearch(Object[],Object)
 * Arrays.binarySearch} in the JDK.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SearchIndexUnknown.class)
public @interface SearchIndexFor {
  /**
   * Sequences for which the annotated expression has the type of the result of a call to {@link
   * java.util.Arrays#binarySearch(Object[],Object) Arrays.binarySearch}.
   */
  @JavaExpression
  public String[] value();
}
