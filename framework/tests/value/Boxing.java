import org.checkerframework.common.value.qual.*;

public class Boxing {

  void simpleTest1(@BottomVal Integer x, int y) {
    @BottomVal int f = x.intValue();
    if (x.intValue() == y) {
      @BottomVal int z = y;
    }
  }

  void simpleTest2(@BottomVal Integer x, int y) {
    if (x == y) {
      @BottomVal int z = y;
    }
  }

  void simpleTest3(@BottomVal Integer x, Integer y) {
    if (x == y) {
      @BottomVal int z = y;
    }
  }
}
