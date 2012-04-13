import checkers.util.test.*;

class ListOuter<T> { }

public class InnerGenerics {
    class ListInner<T> { }

    void testInner() {
      @Odd ListOuter<String> o = new @Odd ListOuter<String>();
      @Odd ListInner<String> i = new @Odd ListInner<String>();
  }

}
