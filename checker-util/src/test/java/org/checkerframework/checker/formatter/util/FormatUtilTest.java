package org.checkerframework.checker.formatter.util;

import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link FormatUtil}, verifying that the relative index flag ({@code <}) takes precedence
 * over an explicit argument index when both appear in the same format specifier.
 *
 * <p>According to the Java {@link java.util.Formatter} specification, the {@code <} flag causes a
 * format specifier to reuse the argument from the previous specifier, regardless of any explicit
 * index. For example, {@code String.format("%2$s %1$<s", "a", "b")} produces {@code "b b"}, not
 * {@code "b a"}.
 */
public final class FormatUtilTest {

  /**
   * Verifies that {@code %1$<s} is treated as a relative index, not an explicit reference to
   * argument 1.
   *
   * <p>Format string: {@code "%2$s %1$<s"} with arguments {@code ("a", "b")}.
   *
   * <ul>
   *   <li>{@code %2$s} explicitly references argument 2 (index 1).
   *   <li>{@code %1$<s} should reuse the previous argument (index 1) due to the {@code <} flag,
   *       ignoring the explicit {@code 1$}.
   * </ul>
   *
   * <p>Therefore argument 1 (index 0) is UNUSED. Before the fix, FormatUtil checked for an
   * explicit index first, incorrectly treating {@code %1$<s} as a reference to argument 1.
   */
  @Test
  public void testRelativeIndex_precedenceOverExplicitIndex() {
    ConversionCategory[] categories = FormatUtil.formatParameterCategories("%2$s %1$<s");
    Assert.assertEquals(2, categories.length);
    Assert.assertEquals(ConversionCategory.UNUSED, categories[0]);
    Assert.assertEquals(ConversionCategory.GENERAL, categories[1]);
  }

  /**
   * Same precedence check as above but with a higher explicit index ({@code 3$}), ensuring the fix
   * is not specific to index 1.
   *
   * <p>Format string: {@code "%2$s %3$<d"}.
   *
   * <ul>
   *   <li>{@code %2$s} explicitly references argument 2 (index 1) as GENERAL.
   *   <li>{@code %3$<d} should reuse the previous argument (index 1) due to the {@code <} flag,
   *       intersecting GENERAL with INT to yield INT.
   * </ul>
   *
   * <p>Only 2 arguments are needed (index 0 is UNUSED, index 1 is INT), not 3.
   */
  @Test
  public void testRelativeIndex_precedenceOverExplicitIndex_higherIndex() {
    ConversionCategory[] categories = FormatUtil.formatParameterCategories("%2$s %3$<d");
    Assert.assertEquals(2, categories.length);
    Assert.assertEquals(ConversionCategory.UNUSED, categories[0]);
    Assert.assertEquals(ConversionCategory.INT, categories[1]);
  }
}
