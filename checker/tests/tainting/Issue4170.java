import java.util.ArrayList;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// @below-java10-jdk-skip-test
public class Issue4170 {
  public void method1() {
    var list = new ArrayList<@Untainted String>();
    ArrayList<@Untainted String> list2 = list;
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
}
