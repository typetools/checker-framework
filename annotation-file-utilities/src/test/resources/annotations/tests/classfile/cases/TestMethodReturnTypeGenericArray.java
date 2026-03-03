package annotations.tests.classfile.cases;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestMethodReturnTypeGenericArray {

  public List test() {
    return null;
  }

  public List<String> test2() {
    return null;
  }

  public String[] test3() {
    return null;
  }

  public String[][] test4() {
    return null;
  }

  public Set<String[]> test5() {
    return null;
  }

  public Map<Map<String[], Set<String>>, Set<String[]>> test6() {
    return null;
  }
}
