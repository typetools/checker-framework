package annotations.tests.classfile.cases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TestObjectCreation {
  public Object o;

  public void test() {
    o = new Object();
    o = new String();
    o = new String("");
  }

  public void test2() {
    o = "str";
    o = new ArrayList();
  }

  public void test3() {
    o = new HashSet();
    o = new HashMap();
  }

  public void test4() {
    o = new String("hello");
    o = new TestObjectCreation();
  }
}
