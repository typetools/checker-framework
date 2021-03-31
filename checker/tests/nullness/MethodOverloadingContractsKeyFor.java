import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.dataflow.qual.Pure;

public class MethodOverloadingContractsKeyFor {

  static class ClassA {}

  static class ClassB extends ClassA {}

  @Pure
  String name(ClassA classA) {
    return "asClassA";
  }

  @Pure
  Object name(ClassB classB) {
    return "asClassB";
  }

  Map<Object, Object> map = new HashMap<>();

  @EnsuresKeyFor(value = "name(#1)", map = "map")
  void put(ClassA classA) {
    map.put(name(classA), "");
  }

  void test(ClassA classA, ClassB classB) {
    put(classA);
    map.get(name(classA)).toString();

    put(classB);
    // :: error: (dereference.of.nullable)
    map.get(name(classB)).toString();
  }

  public static void main(String[] args) {
    new MethodOverloadingContractsKeyFor().test(new ClassA(), new ClassB());
  }
}
