package org.checkerframework.checker.i18nformatter.unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.junit.Test;

public class I18nConversionCategoryTest {

    @Test
    public void StringToI18nConversionCategoryTest() {
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
    public void IsSubsetTest() {

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
}
