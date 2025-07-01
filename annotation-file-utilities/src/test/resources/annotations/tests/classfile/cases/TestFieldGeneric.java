package annotations.tests.classfile.cases;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestFieldGeneric<T> {
  String s;
  List<String> list;
  Set<TestFieldGeneric> set;
  TestFieldGeneric<T> testFieldGeneric = new TestFieldGeneric<>();

  public TestFieldGeneric() {}

  Set<String> otherSet;

  public String toString() {
    return s;
  }

  Set<TestFieldGeneric<Set<TestFieldGeneric>>> nestedSet;

  Map<Set<TestFieldGeneric>, TestFieldGeneric<T>> nestedMap;
}
