// @below-java17-jdk-skip-test
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingBindingVariable {

  void bar(@Untainted Object o) {
    if (o instanceof String s) {
      @Untainted String f = s;
    }
  }
  void bar2( Object o) {
    if (o instanceof @Untainted String s) { // error or warning?
      @Untainted String f = s;
    }
  }
}
