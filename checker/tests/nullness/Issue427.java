// Test case for Issue 427:
// https://code.google.com/p/checker-framework/issues/detail?id=427

//We need to add a warning when an @AssumeAssertion is missing
//its @ symbol (as below)

// @skip-test

import org.checkerframework.checker.nullness.qual.*;

import java.util.*;

public class Issue427 {

  public static void assumeAssertionKeyFor1(String var, Map<String, Integer> m) {
    assert m.containsKey(var) : "@AssumeAssertion(keyfor): keys of leaders and timeKilled are the same";
    boolean b = (m.get(var) >= 22);
  }

  public static void assumeAssertionKeyFor2(String var, Map<String, Integer> m) {
    assert m.containsKey(var) : "@AssumeAssertion(keyfor): keys of leaders and timeKilled are the same";
    int x = m.get(var);
  }

}
