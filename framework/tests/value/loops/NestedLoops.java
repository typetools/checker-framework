import org.checkerframework.common.value.qual.IntRange;

// Because the analysis of loops isn't precise enough, the Value Checker issues
// warnings on this test case. So, suppress those warnings, but run the tests
// to make sure that dataflow reaches a fixed point.
@SuppressWarnings("value")
public class NestedLoops {
  void test1() {
    int doWhileIndex = 0;
    do {
      for (int forIndex = 0; forIndex < doWhileIndex; forIndex++) {
        System.out.print("Hello");
        int whileIndex = 0;
        while (whileIndex < forIndex) {
          whileIndex++;
        }
      }
      doWhileIndex++;
    } while (doWhileIndex < Integer.MAX_VALUE);
  }

  void test2() {
    int doWhileIndex = 0;
    do {
      for (int forIndex = 0; forIndex < Integer.MAX_VALUE; forIndex++) {
        System.out.print("Hello");
        int whileIndex = 0;
        while (whileIndex < Integer.MAX_VALUE) {
          whileIndex++;
        }
      }
      doWhileIndex++;
    } while (doWhileIndex < Integer.MAX_VALUE);
  }

  void test3() {
    int doWhileIndex = 0;
    int forIndex;
    int whileIndex = 0;

    do {
      @IntRange(to = 2999) int a = doWhileIndex;
      for (forIndex = 0; forIndex < 4000; forIndex++) {

        @IntRange(to = 3999) int b = forIndex;
        System.out.print("Hello");
        whileIndex = 0;
        while (whileIndex < 5000) {
          @IntRange(to = 4999) int c = whileIndex;
          whileIndex++;
        }
      }
      doWhileIndex++;
    } while (doWhileIndex < 3000);
  }
}
