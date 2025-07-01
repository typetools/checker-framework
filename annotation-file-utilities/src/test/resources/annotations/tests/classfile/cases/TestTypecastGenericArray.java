package annotations.tests.classfile.cases;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestTypecastGenericArray {
  public Object o;
  public String s;
  public Integer i;
  public Boolean b;
  public Set<String> set;
  public HashSet<Set<String>> hset;
  public Map<Set<String>,Set<Map<String,Set<String>>>> map;

  public void test() {
    o = (Object) o;
    o = (Object) s;
    s = (String) o;
    i = (Integer) o;
    b = (Boolean) b;
  }

  @SuppressWarnings({"unchecked"})
  public void test2() {
    set = (HashSet<String>) o;
    set = (Set<String>) o;
  }

  @SuppressWarnings({"unchecked"})
  public void test3() {
    set = (HashSet<String>) map.keySet().iterator().next();
    hset = (HashSet<Set<String>>) o;
  }

  @SuppressWarnings("unchecked")
  public void test4() {
    map = (Map<Set<String>, Set<Map<String, Set<String>>>>) o;
    Set<Map<String, Set<String>>> t = (Set<Map<String, Set<String>>>) o;
    set = map.get(null).iterator().next().get("");
  }

  @SuppressWarnings("unchecked")
  public void test5() {
    Map<String, String[][]> m;
    m = (Map<String, String[][]>) o;
    System.out.println(m);
  }
}
