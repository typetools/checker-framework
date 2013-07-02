import java.util.ArrayList;
import java.util.List;

class Raw3 {

  List<String> foo1() {
    List<String> sl = new ArrayList<>();
    return (List) sl;
  }

  List<String> foo2() {
    List<String> sl = new ArrayList<>();
    return (List<String>) sl;
  }

  class TestList<T> {
    List<String> foo3() {
      List<String> sl = new ArrayList<>();
      return (List) sl;
    }
    List<String> foo4() {
      List<String> sl = new ArrayList<>();
      return (List<String>) sl;
    }
  }

}
