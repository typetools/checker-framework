// This class should be kept in sync with org.plumelib.util.RegexUtilTest in the plume-util project.

package org.checkerframework.checker.regex.util;

import org.junit.Assert;
import org.junit.Test;

public final class RegexUtilTest {

    @Test
    public void test_isRegex_and_asRegex() {

        String s1 = "colo(u?)r";
        String s2 = "(brown|beige)";
        String s3 = "colou?r";
        String s4 = "1) first point";

        Assert.assertTrue(RegexUtil.isRegex(s1));
        RegexUtil.asRegex(s1);
        Assert.assertTrue(RegexUtil.isRegex(s1, 0));
        RegexUtil.asRegex(s1, 0);
        Assert.assertTrue(RegexUtil.isRegex(s1, 1));
        RegexUtil.asRegex(s1, 1);
        Assert.assertFalse(RegexUtil.isRegex(s1, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s1, 2));

        Assert.assertTrue(RegexUtil.isRegex(s2));
        RegexUtil.asRegex(s2);
        Assert.assertTrue(RegexUtil.isRegex(s2, 0));
        RegexUtil.asRegex(s2, 0);
        Assert.assertTrue(RegexUtil.isRegex(s2, 1));
        RegexUtil.asRegex(s2, 1);
        Assert.assertFalse(RegexUtil.isRegex(s2, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s2, 2));

        Assert.assertTrue(RegexUtil.isRegex(s3));
        RegexUtil.asRegex(s3);
        Assert.assertTrue(RegexUtil.isRegex(s3, 0));
        RegexUtil.asRegex(s3, 0);
        Assert.assertFalse(RegexUtil.isRegex(s3, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s3, 1));
        Assert.assertFalse(RegexUtil.isRegex(s3, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s3, 2));

        Assert.assertFalse(RegexUtil.isRegex(s4));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4));
        Assert.assertFalse(RegexUtil.isRegex(s4, 0));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 0));
        Assert.assertFalse(RegexUtil.isRegex(s4, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 1));
        Assert.assertFalse(RegexUtil.isRegex(s4, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 2));
    }
}
