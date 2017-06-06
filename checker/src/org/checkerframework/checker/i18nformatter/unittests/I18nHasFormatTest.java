package org.checkerframework.checker.i18nformatter.unittests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.checkerframework.checker.i18nformatter.I18nFormatUtil;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.junit.Test;

public class I18nHasFormatTest {

    @Test
    public void alltests() {
        assertTrue(I18nFormatUtil.hasFormat("{0}", I18nConversionCategory.GENERAL));
        assertTrue(I18nFormatUtil.hasFormat("{0, date}", I18nConversionCategory.DATE));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{1} {0, date}",
                        I18nConversionCategory.NUMBER, I18nConversionCategory.NUMBER));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{0} and {1,number}",
                        I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{1, number}",
                        I18nConversionCategory.UNUSED,
                        I18nConversionCategory.NUMBER));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{1, date}", I18nConversionCategory.UNUSED, I18nConversionCategory.DATE));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{2}",
                        I18nConversionCategory.UNUSED,
                        I18nConversionCategory.UNUSED,
                        I18nConversionCategory.NUMBER));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{3, number} {0} {1, time}",
                        I18nConversionCategory.GENERAL,
                        I18nConversionCategory.DATE,
                        I18nConversionCategory.UNUSED,
                        I18nConversionCategory.NUMBER));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{0} {1, date} {2, time} {3, number} {5}",
                        I18nConversionCategory.GENERAL,
                        I18nConversionCategory.DATE,
                        I18nConversionCategory.DATE,
                        I18nConversionCategory.NUMBER,
                        I18nConversionCategory.UNUSED,
                        I18nConversionCategory.GENERAL));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{1} {1, date}",
                        I18nConversionCategory.UNUSED, I18nConversionCategory.DATE));
        assertTrue(
                I18nFormatUtil.hasFormat(
                        "{1, number} {1, date}",
                        I18nConversionCategory.UNUSED,
                        I18nConversionCategory.NUMBER));
        assertTrue(I18nFormatUtil.hasFormat("{0, date} {0, date}", I18nConversionCategory.DATE));

        assertFalse(I18nFormatUtil.hasFormat("{1}", I18nConversionCategory.GENERAL));
        assertFalse(I18nFormatUtil.hasFormat("{0, number}", I18nConversionCategory.DATE));
        assertFalse(I18nFormatUtil.hasFormat("{0, number}", I18nConversionCategory.GENERAL));
        assertFalse(I18nFormatUtil.hasFormat("{0, date}", I18nConversionCategory.GENERAL));
        assertFalse(
                I18nFormatUtil.hasFormat(
                        "{0, date}", I18nConversionCategory.DATE, I18nConversionCategory.DATE));
        assertFalse(I18nFormatUtil.hasFormat("{0, date} {1, date}", I18nConversionCategory.DATE));
    }
}
