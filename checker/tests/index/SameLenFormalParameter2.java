import org.checkerframework.checker.index.qual.*;

public class SameLenFormalParameter2 {

  void lib(Object @SameLen({"#1", "#2"}) [] valsArg, int @SameLen({"#1", "#2"}) [] modsArg) {}

  void client(Object[] myvals, int[] mymods) {
    // :: error: (argument)
    lib(myvals, mymods);
  }
}
