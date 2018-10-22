package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.checkerframework.checker.i18nformatter.I18nFormatUtil;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.junit.Test;

public class I18nFormatterUnitTest {

    @Test
    public void stringToI18nConversionCategoryTest() {
        assertEquals(
                I18nConversionCategory.NUMBER,
                I18nConversionCategory.stringToI18nConversionCategory("number"));
        assertEquals(
                I18nConversionCategory.NUMBER,
                I18nConversionCategory.stringToI18nConversionCategory("nuMber"));
        assertEquals(
                I18nConversionCategory.NUMBER,
                I18nConversionCategory.stringToI18nConversionCategory("choice"));
        assertEquals(
                I18nConversionCategory.DATE,
                I18nConversionCategory.stringToI18nConversionCategory("TIME"));
        assertEquals(
                I18nConversionCategory.DATE,
                I18nConversionCategory.stringToI18nConversionCategory("DatE"));
        assertEquals(
                I18nConversionCategory.DATE,
                I18nConversionCategory.stringToI18nConversionCategory("date"));
    }

    @Test
    public void isSubsetTest() {

        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.UNUSED, I18nConversionCategory.UNUSED));
        assertFalse(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.UNUSED, I18nConversionCategory.GENERAL));
        assertFalse(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.UNUSED, I18nConversionCategory.DATE));
        assertFalse(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.UNUSED, I18nConversionCategory.NUMBER));

        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.GENERAL, I18nConversionCategory.UNUSED));
        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.GENERAL, I18nConversionCategory.GENERAL));
        assertFalse(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.GENERAL, I18nConversionCategory.DATE));
        assertFalse(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER));

        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.DATE, I18nConversionCategory.UNUSED));
        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.DATE, I18nConversionCategory.GENERAL));
        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.DATE, I18nConversionCategory.DATE));
        assertFalse(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.DATE, I18nConversionCategory.NUMBER));

        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.NUMBER, I18nConversionCategory.UNUSED));
        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.NUMBER, I18nConversionCategory.GENERAL));
        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.NUMBER, I18nConversionCategory.DATE));
        assertTrue(
                I18nConversionCategory.isSubsetOf(
                        I18nConversionCategory.NUMBER, I18nConversionCategory.NUMBER));
    }

    @Test
    public void hasFormatTest() {
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
