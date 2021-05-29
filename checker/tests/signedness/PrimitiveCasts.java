import org.checkerframework.checker.signedness.qual.Unsigned;

public class PrimitiveCasts {

  void shortToChar1(short s) {
    // :: warning: (cast.unsafe)
    char c = (char) s;
  }

  // These are Java errors.
  // void shortToChar2(short s) {
  //     char c = s;
  // }
  // char shortToChar3(short s) {
  //     return s;
  // }

  void intToDouble1(@Unsigned int ui) {
    double d = (double) ui;
  }

  void intToDouble2(@Unsigned int ui) {
    double d = ui;
  }

  double intToDouble3(@Unsigned int ui) {
    return ui;
  }

  void shortToDouble1(@Unsigned short ui) {
    double d = (double) ui;
  }

  void shortToDouble2(@Unsigned short ui) {
    double d = ui;
  }

  double shortToDouble3(@Unsigned short ui) {
    return ui;
  }
}
