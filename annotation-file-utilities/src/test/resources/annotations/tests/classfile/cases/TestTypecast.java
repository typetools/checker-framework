package annotations.tests.classfile.cases;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestTypecast {
  public Object o;
  public String s;
  public Integer i;
  public Boolean b;
  public Set set;
  public HashSet hset;
  public Map map;

  public void test() {
    o = (Object) o;
    o = (Object) s;
    s = (String) o;
    i = (Integer) o;
    b = (Boolean) b;
    set = (HashSet) hset;
    hset = (HashSet) set;
    map = (Map) hset;
    int pi = 0;
    i = pi;
    o = pi;
    o = (String & Comparable<String>) o;
    o = (String & Map<String, ? extends Set<String>> & CharSequence) o;
  }
}
