package annotations.tests.classfile.cases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestTypeTest {
  public Object o;

  public void test() {
    if (o instanceof Map) {
      if (o instanceof Set) {
        if (o instanceof List) {
          o = new Object();
        }
      }
    }
  }

  public void test2() {
    if (o instanceof List) {
      if (o instanceof ArrayList) {
        o = new Object();
      }
    }
  }

  public void test3() {
    if (!(o instanceof Object)) {
      o = new Object();
    }
  }

  public void test4() {
    Class c = o.getClass();
    if (o instanceof Boolean) {
      c = Boolean.class;
    } else if (o instanceof Integer) {
      c = Integer.class;
    } else if (o instanceof Character) {
      c = Character.class;
    } else if (o instanceof String) {
      c = String.class;
    } else if (o instanceof List) {
      c = List.class;
    } else {
      c = int.class;
    }
    System.out.println(c);
  }
}
