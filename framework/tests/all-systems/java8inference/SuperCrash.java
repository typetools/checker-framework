import java.util.Collections;
import java.util.List;

public class SuperCrash {
  @FunctionalInterface
  public interface FuncInter<R> {
    R func(R r1, R r2);
  }

  public static class MyClass {
    public static List<MyClass> mergeLists(List<MyClass> list1, List<MyClass> list2) {
      throw new RuntimeException();
    }
  }

  public static class SuperClass<R> {
    protected SuperClass(FuncInter<R> funcInter, R defaultResult) {}
  }

  static class SubClass extends SuperClass<List<MyClass>> {

    private SubClass() {
      super(MyClass::mergeLists, Collections.emptyList());
    }
  }
}
