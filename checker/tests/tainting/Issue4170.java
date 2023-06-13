import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// @below-java11-jdk-skip-test
public class Issue4170 {
  public void method1() {
    var list = new ArrayList<@Untainted String>();
    ArrayList<@Untainted String> list2 = list;
    // :: error: assignment
    ArrayList<String> list3 = new ArrayList<@Untainted String>();
    ArrayList<@Tainted String> list4 = list3;
    var stream = list.stream();
  }

  public void method2() {
    var list = new ArrayList<String>();
    var stream = list.stream();
  }

  public void method3() {
    var list = new ArrayList<@Tainted String>();
    var stream = list.stream();
  }

  public ArrayList<@Untainted String> method4() {
    var list = new ArrayList<@Untainted String>();
    return list;
  }

  public ArrayList<@Tainted String> method5() {
    var list = new ArrayList<@Untainted String>();
    // :: error: (return)
    return list;
  }

  public ArrayList<@Untainted String> method6() {
    var list = new ArrayList<String>();
    // :: error: (return)
    return list;
  }

  public void method7() {
    var list = new ArrayList<@Untainted String>();
    method8(list);
  }

  public void method8(ArrayList<@Untainted String> data) {}

  public void method9(List<@Tainted String> taintedlist, List<@Untainted String> untaintedList) {
    var list1 = taintedlist;
    List<@Tainted String> l = list1;
    // :: error: (assignment)
    list1 = untaintedList;
  }
}
