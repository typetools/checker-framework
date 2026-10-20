package annotations.tests.classfile.cases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestObjectCreationGenericArray {
  public Object o;

  public void test() {
    o = new int[10];
  }

  public void test2() {
    o = "str";
    o = new ArrayList<String>();
  }

  public void test3() {
    o = new HashSet<Map<String, String>>();
    o = new HashMap<String, Set<String[]>>();
  }

  public void test4() {
    o = new HashMap<String[], Set<String[]>>();
  }
}
