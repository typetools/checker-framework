package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.util.I18nFormatUtil;
import org.junit.Assert;
import org.junit.Test;

public class I18nFormatterUnitTest {

  @Test
  public void stringToI18nConversionCategoryTest() {
    Assert.assertEquals(
        I18nConversionCategory.NUMBER,
        I18nConversionCategory.stringToI18nConversionCategory("number"));
    Assert.assertEquals(
        I18nConversionCategory.NUMBER,
        I18nConversionCategory.stringToI18nConversionCategory("nuMber"));
    Assert.assertEquals(
        I18nConversionCategory.NUMBER,
        I18nConversionCategory.stringToI18nConversionCategory("choice"));
    Assert.assertEquals(
        I18nConversionCategory.DATE, I18nConversionCategory.stringToI18nConversionCategory("TIME"));
    Assert.assertEquals(
        I18nConversionCategory.DATE, I18nConversionCategory.stringToI18nConversionCategory("DatE"));
    Assert.assertEquals(
        I18nConversionCategory.DATE, I18nConversionCategory.stringToI18nConversionCategory("date"));
  }

  @Test
  public void isSubsetTest() {

    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.UNUSED, I18nConversionCategory.UNUSED));
    Assert.assertFalse(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.UNUSED, I18nConversionCategory.GENERAL));
    Assert.assertFalse(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.UNUSED, I18nConversionCategory.DATE));
    Assert.assertFalse(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.UNUSED, I18nConversionCategory.NUMBER));

    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.GENERAL, I18nConversionCategory.UNUSED));
    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.GENERAL, I18nConversionCategory.GENERAL));
    Assert.assertFalse(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.GENERAL, I18nConversionCategory.DATE));
    Assert.assertFalse(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER));

    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.DATE, I18nConversionCategory.UNUSED));
    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.DATE, I18nConversionCategory.GENERAL));
    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.DATE, I18nConversionCategory.DATE));
    Assert.assertFalse(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.DATE, I18nConversionCategory.NUMBER));

    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.NUMBER, I18nConversionCategory.UNUSED));
    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.NUMBER, I18nConversionCategory.GENERAL));
    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.NUMBER, I18nConversionCategory.DATE));
    Assert.assertTrue(
        I18nConversionCategory.isSubsetOf(
            I18nConversionCategory.NUMBER, I18nConversionCategory.NUMBER));
  }

  @Test
  public void hasFormatTest() {
    Assert.assertTrue(I18nFormatUtil.hasFormat("{0}", I18nConversionCategory.GENERAL));
    Assert.assertTrue(I18nFormatUtil.hasFormat("{0, date}", I18nConversionCategory.DATE));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{1} {0, date}", I18nConversionCategory.NUMBER, I18nConversionCategory.NUMBER));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{0} and {1,number}", I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{1, number}", I18nConversionCategory.UNUSED, I18nConversionCategory.NUMBER));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{1, date}", I18nConversionCategory.UNUSED, I18nConversionCategory.DATE));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{2}",
            I18nConversionCategory.UNUSED,
            I18nConversionCategory.UNUSED,
            I18nConversionCategory.NUMBER));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{3, number} {0} {1, time}",
            I18nConversionCategory.GENERAL,
            I18nConversionCategory.DATE,
            I18nConversionCategory.UNUSED,
            I18nConversionCategory.NUMBER));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{0} {1, date} {2, time} {3, number} {5}",
            I18nConversionCategory.GENERAL,
            I18nConversionCategory.DATE,
            I18nConversionCategory.DATE,
            I18nConversionCategory.NUMBER,
            I18nConversionCategory.UNUSED,
            I18nConversionCategory.GENERAL));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{1} {1, date}", I18nConversionCategory.UNUSED, I18nConversionCategory.DATE));
    Assert.assertTrue(
        I18nFormatUtil.hasFormat(
            "{1, number} {1, date}", I18nConversionCategory.UNUSED, I18nConversionCategory.NUMBER));
    Assert.assertTrue(I18nFormatUtil.hasFormat("{0, date} {0, date}", I18nConversionCategory.DATE));

    Assert.assertFalse(I18nFormatUtil.hasFormat("{1}", I18nConversionCategory.GENERAL));
    Assert.assertFalse(I18nFormatUtil.hasFormat("{0, number}", I18nConversionCategory.DATE));
    Assert.assertFalse(I18nFormatUtil.hasFormat("{0, number}", I18nConversionCategory.GENERAL));
    Assert.assertFalse(I18nFormatUtil.hasFormat("{0, date}", I18nConversionCategory.GENERAL));
    Assert.assertFalse(
        I18nFormatUtil.hasFormat(
            "{0, date}", I18nConversionCategory.DATE, I18nConversionCategory.DATE));
    Assert.assertFalse(
        I18nFormatUtil.hasFormat("{0, date} {1, date}", I18nConversionCategory.DATE));
  }
}
