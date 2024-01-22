// This class should be kept in sync with org.plumelib.util.RegexUtilTest in the plume-util project.

package org.checkerframework.checker.regex.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.regex.qual.Regex;
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

  List<String> s1 = Arrays.asList(new String[] {"a", "b", "c"});
  List<String> s2 = Arrays.asList(new String[] {"a", "b", "c", "d"});
  List<String> s3 = Arrays.asList(new String[] {"aa", "bb", "cc"});
  List<String> s4 = Arrays.asList(new String[] {"a", "aa", "b", "bb", "c"});
  List<String> s5 = Arrays.asList(new String[] {"d", "ee", "fff"});
  List<String> s6 = Arrays.asList(new String[] {"a", "d", "ee", "fff"});

  List<@Regex String> r1 = Arrays.asList(new @Regex String[] {});
  List<@Regex String> r2 = Arrays.asList(new @Regex String[] {"a", "b", "c"});
  List<@Regex String> r3 = Arrays.asList(new @Regex String[] {"a+", "b+", "c"});
  List<@Regex String> r4 = Arrays.asList(new @Regex String[] {"a+", "b+", "c+"});
  List<@Regex String> r5 = Arrays.asList(new @Regex String[] {".*"});

  List<@Regex String> r6 = Arrays.asList(new @Regex String[] {"a?b", "a*"});
  List<@Regex String> r7 = Arrays.asList(new @Regex String[] {"a?b+", "a*"});

  List<String> empty = Collections.emptyList();
  List<String> onlyA = Arrays.asList(new String[] {"a"});
  List<String> onlyAA = Arrays.asList(new String[] {"aa"});
  List<String> onlyC = Arrays.asList(new String[] {"c"});
  List<String> onlyCC = Arrays.asList(new String[] {"cc"});
  List<String> onlyD = Arrays.asList(new String[] {"d"});
  List<String> aaab = Arrays.asList(new String[] {"a", "aa", "b"});
  List<String> ab = Arrays.asList(new String[] {"a", "b"});
  List<String> aabb = Arrays.asList(new String[] {"aa", "bb"});
  List<String> aacc = Arrays.asList(new String[] {"aa", "cc"});
  List<String> bbc = Arrays.asList(new String[] {"bb", "c"});
  List<String> bbcc = Arrays.asList(new String[] {"bb", "cc"});
  List<String> cc = Arrays.asList(new String[] {"cc"});
  List<String> cd = Arrays.asList(new String[] {"c", "d"});
  List<String> eefff = Arrays.asList(new String[] {"ee", "fff"});

  @Test
  public void test_matchesSomeRegex() {
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s1, r1), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s2, r1), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s3, r1), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s4, r1), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s5, r1), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s6, r1), empty);

    Assert.assertEquals(RegexUtil.matchesSomeRegex(s1, r2), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s2, r2), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s3, r2), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s4, r2), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s5, r2), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s6, r2), onlyA);

    Assert.assertEquals(RegexUtil.matchesSomeRegex(s1, r3), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s2, r3), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s3, r3), aabb);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s4, r3), s4);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s5, r3), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s6, r3), onlyA);

    Assert.assertEquals(RegexUtil.matchesSomeRegex(s1, r4), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s2, r4), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s3, r4), s3);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s4, r4), s4);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s5, r4), empty);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s6, r4), onlyA);

    Assert.assertEquals(RegexUtil.matchesSomeRegex(s1, r5), s1);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s2, r5), s2);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s3, r5), s3);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s4, r5), s4);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s5, r5), s5);
    Assert.assertEquals(RegexUtil.matchesSomeRegex(s6, r5), s6);

    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s1, r1));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s2, r1));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s3, r1));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s4, r1));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s5, r1));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s6, r1));

    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s1, r2));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s2, r2));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s3, r2));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s4, r2));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s5, r2));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s6, r2));

    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s1, r3));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s2, r3));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s3, r3));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s4, r3));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s5, r3));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s6, r3));

    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s1, r4));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s2, r4));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s3, r4));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s4, r4));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s5, r4));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s6, r4));

    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s1, r5));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s2, r5));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s3, r5));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s4, r5));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s5, r5));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(s6, r5));
  }

  @Test
  public void test_matchesNoRegex() {
    Assert.assertEquals(RegexUtil.matchesNoRegex(s1, r1), s1);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s2, r1), s2);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r1), s3);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s4, r1), s4);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s5, r1), s5);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s6, r1), s6);

    Assert.assertEquals(RegexUtil.matchesNoRegex(s1, r2), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s2, r2), onlyD);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r2), s3);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s4, r2), aabb);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s5, r2), s5);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s6, r2), s5);

    Assert.assertEquals(RegexUtil.matchesNoRegex(s1, r3), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s2, r3), onlyD);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r3), cc);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s4, r3), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s5, r3), s5);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s6, r3), s5);

    Assert.assertEquals(RegexUtil.matchesNoRegex(s1, r4), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s2, r4), onlyD);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r4), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s4, r4), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s5, r4), s5);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s6, r4), s5);

    Assert.assertEquals(RegexUtil.matchesNoRegex(s1, r5), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s2, r5), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r5), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s4, r5), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s5, r5), empty);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s6, r5), empty);

    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s1, r1));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s2, r1));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s3, r1));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s4, r1));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s5, r1));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s6, r1));

    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s1, r2));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s2, r2));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s3, r2));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s4, r2));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s5, r2));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s6, r2));

    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s1, r3));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s2, r3));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s3, r3));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s4, r3));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s5, r3));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s6, r3));

    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s1, r4));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s2, r4));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s3, r4));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s4, r4));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s5, r4));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s6, r4));

    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s1, r5));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s2, r5));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s3, r5));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s4, r5));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s5, r5));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s6, r5));
  }

  @Test
  public void test_r6() {
    Assert.assertEquals(ab, RegexUtil.matchesSomeRegex(s1, r6));
    Assert.assertEquals(ab, RegexUtil.matchesSomeRegex(s2, r6));
    Assert.assertEquals(onlyAA, RegexUtil.matchesSomeRegex(s3, r6));
    Assert.assertEquals(aaab, RegexUtil.matchesSomeRegex(s4, r6));
    Assert.assertEquals(empty, RegexUtil.matchesSomeRegex(s5, r6));
    Assert.assertEquals(onlyA, RegexUtil.matchesSomeRegex(s6, r6));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s1, r6));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s2, r6));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s3, r6));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s4, r6));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s5, r6));
    Assert.assertFalse(RegexUtil.everyStringMatchesSomeRegex(s6, r6));
    Assert.assertTrue(RegexUtil.everyStringMatchesSomeRegex(onlyA, r7));
    Assert.assertEquals(RegexUtil.matchesNoRegex(s1, r6), onlyC);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s2, r6), cd);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r6), bbcc);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s3, r7), onlyCC);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s4, r6), bbc);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s5, r6), s5);
    Assert.assertEquals(RegexUtil.matchesNoRegex(s6, r6), s5);
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s1, r6));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s2, r6));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s3, r6));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s4, r6));
    Assert.assertTrue(RegexUtil.noStringMatchesAnyRegex(s5, r6));
    Assert.assertFalse(RegexUtil.noStringMatchesAnyRegex(s6, r6));
  }
}
