// This class should be kept in sync with org.plumelib.util.RegexUtilTest in the plume-util project.

package org.checkerframework.checker.regex.util;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public final class RegexUtilTest {

    @Test
    public void test_isRegex_and_asRegex() {

        String s1 = "colo(u?)r";
        String s2 = "(brown|beige)";
        String s3 = "colou?r";
        String s4 = "1) first point";
        String s5 = "(abc)(cde[)])(\\Qxyz\\E)";

        Assert.assertTrue(RegexUtil.isRegex(s1));
        RegexUtil.asRegex(s1);
        Assert.assertTrue(RegexUtil.isRegex(s1, 0));
        RegexUtil.asRegex(s1, 0);
        Assert.assertTrue(RegexUtil.isRegex(s1, 1));
        RegexUtil.asRegex(s1, 1);
        Assert.assertFalse(RegexUtil.isRegex(s1, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s1, 2));
        Assert.assertTrue(RegexUtil.isRegex(s1, 1, 1));
        Assert.assertFalse(RegexUtil.isRegex(s1, 1, 1, 2));
        Assert.assertFalse(RegexUtil.isRegex(s1, 2, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s1, 2, 1));

        Assert.assertTrue(RegexUtil.isRegex(s2));
        RegexUtil.asRegex(s2);
        Assert.assertTrue(RegexUtil.isRegex(s2, 0));
        RegexUtil.asRegex(s2, 0);
        Assert.assertTrue(RegexUtil.isRegex(s2, 1));
        RegexUtil.asRegex(s2, 1);
        Assert.assertFalse(RegexUtil.isRegex(s2, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s2, 2));
        Assert.assertTrue(RegexUtil.isRegex(s2, 1, 1));
        Assert.assertFalse(RegexUtil.isRegex(s2, 1, 1, 2));
        Assert.assertFalse(RegexUtil.isRegex(s2, 2, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s2, 2, 1));

        Assert.assertTrue(RegexUtil.isRegex(s3));
        RegexUtil.asRegex(s3);
        Assert.assertTrue(RegexUtil.isRegex(s3, 0));
        RegexUtil.asRegex(s3, 0);
        Assert.assertFalse(RegexUtil.isRegex(s3, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s3, 1));
        Assert.assertFalse(RegexUtil.isRegex(s3, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s3, 2));
        Assert.assertFalse(RegexUtil.isRegex(s3, 0, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s3, 0, 1));

        Assert.assertFalse(RegexUtil.isRegex(s4));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4));
        Assert.assertFalse(RegexUtil.isRegex(s4, 0));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 0));
        Assert.assertFalse(RegexUtil.isRegex(s4, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 1));
        Assert.assertFalse(RegexUtil.isRegex(s4, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 2));
        Assert.assertFalse(RegexUtil.isRegex(s4, 1, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s4, 1, 1));

        Assert.assertTrue(RegexUtil.isRegex(s5, 3, 1, 2, 3));
        Assert.assertTrue(RegexUtil.isRegex(s5, 3, 1, 2));
        Assert.assertTrue(RegexUtil.isRegex(s5, 3, 1));
        Assert.assertFalse(RegexUtil.isRegex(s5, 4, 1, 2, 3));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s5, 3, 1, 2, 4));
        Assert.assertThrows(Error.class, () -> RegexUtil.asRegex(s5, 4, 1, 2, 3));
    }

    @Test
    public void test_getNonNullGroups() {
        String s1 = "\\(abc\\)?(123)";
        String s2 = "([(abc)])*(xyz)\\?(abc)";
        String s3 = "(\\Q()()\\E)";
        String s4 = "([abc\\Qwww\\E])(abc)?";
        String s5 = "[(abc]";

        Assert.assertThrows(Error.class, () -> RegexUtil.getNonNullGroups(s1, 2));
        Assert.assertEquals(Collections.singletonList(1), RegexUtil.getNonNullGroups(s1, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.getNonNullGroups(s2, 4));
        Assert.assertEquals(Arrays.asList(2, 3), RegexUtil.getNonNullGroups(s2, 3));
        Assert.assertThrows(Error.class, () -> RegexUtil.getNonNullGroups(s3, 3));
        Assert.assertEquals(Collections.singletonList(1), RegexUtil.getNonNullGroups(s3, 1));
        Assert.assertThrows(Error.class, () -> RegexUtil.getNonNullGroups(s4, 3));
        Assert.assertEquals(Collections.singletonList(1), RegexUtil.getNonNullGroups(s4, 2));
        Assert.assertThrows(Error.class, () -> RegexUtil.getNonNullGroups(s5, 1));
    }
}
