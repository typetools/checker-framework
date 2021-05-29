import java.util.Date;
import java.util.List;
import org.checkerframework.framework.testchecker.util.*;

public class Wildcards {
  void process(List<? extends Date> arg) {}

  void test() {
    List<? extends @Odd Date> myList = null;
    process(myList);
  }
}
