package annotations.tests.classfile.cases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestLocalVariableGenericArray {

  Integer i;

  Map<String, Set<String>> map1;

  Map<String, ArrayList<Map<String, String>>> map2;

  public TestLocalVariableGenericArray() {
    int k = 1;
    for (Map<String, String> e : map2.get("4gf")) {
      if (k < 5) {
        k = map2.get("").indexOf(new ArrayList<Map<String,String>>());
      } else {
        k = this.i.intValue() + 5;
      }
      k++;
    }
  }

  public void someMethod() {
    Set<String> s = new HashSet<>();
    s.add(new String());
    s.add(s.toString());
  }

  public int someMethod2(int i) {
    Set<Boolean> s = new HashSet<>();
    Set<Integer> ints = new HashSet<>();
    boolean b = someMethod3();
    if (s.iterator().next() & b) {
      return b ? i : ints.iterator().next();
    }
    return i;
  }

  public boolean someMethod3() {
    Map<String, Set<String>> t = new HashMap<>();
    Map<String, Set<Map<Integer, String[][]>>> s = null;

    s.get("3").add(new HashMap<>());

    s.get("4").iterator().next().get(3)[2][4] = "Hello";

    return true;
  }

  protected void someMethod4() {
    try {
      Set<String> s = new HashSet<>();
      Map<Set<String>, String> m;
      throw new RuntimeException("Hello");
    } catch (Exception e) {
      System.out.println(i);
    }
  }

}
