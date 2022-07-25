// @above-java17-skip-test TODO: reinstate on JDK 18, false positives are probably due to issue #979

package wildcards;

public class Iterate {
  void method(Iterable<? extends Object> files) {
    for (Object file : files) {
      file.getClass();
    }
  }
}
