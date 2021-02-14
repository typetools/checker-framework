package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.formatter.util.FormatUtil;
import org.junit.Assert;
import org.junit.Test;

public class FormatterUnitTest {

    @SuppressWarnings("deprecation") // calls methods that are used only for testing
    @Test
    public void testConversionCharFromFormat() {
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%1$2s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%1$s"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%1$tb"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%1$te"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%1$tm"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%1$tY"));
        Assert.assertEquals('f', FormatUtil.conversionCharFromFormat("%+10.4f"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%2$2s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%2$s"));
        Assert.assertEquals('f', FormatUtil.conversionCharFromFormat("%(,.2f"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%3$2s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%3$s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%4$2s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%4$s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%<s"));
        Assert.assertEquals('s', FormatUtil.conversionCharFromFormat("%s"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%ta"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tb"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tc"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%td"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%<te"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tH"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tI"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tm"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tM"));
        Assert.assertEquals('T', FormatUtil.conversionCharFromFormat("%Tp"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tS"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tT"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%ty"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%<tY"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tY"));
        Assert.assertEquals('t', FormatUtil.conversionCharFromFormat("%tZ"));
    }
}
