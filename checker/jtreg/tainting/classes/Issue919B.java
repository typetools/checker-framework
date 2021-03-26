package classes;

import java.util.Map;
import java.util.Set;

public class Issue919B {

  @SuppressWarnings("nullness:return.type.incompatible")
  public static Map<String, InnerClass> otherMethod(Set<InnerClass> innerClassSet) {
    return null;
  }

  public static class InnerClass {

    InnerClass() {}

    InnerClass method(String a) {
      return new InnerClass();
    }
  }

  // This class is required in order to reproduce the error.
  public static class AnotherInnerClass {}
}
