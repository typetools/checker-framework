import org.checkerframework.checker.signedness.qual.UnknownSignedness;

public class WideningFloat {

  void floatArg(float x) {}

  void m(Object arg) {
    floatArg((Byte) arg);
  }

  void m2(@UnknownSignedness Byte arg) {
    floatArg(arg);
  }

  void m3(@UnknownSignedness Byte arg) {
    float f = arg;
  }

  void m3(@UnknownSignedness byte arg) {
    float f = arg;
  }
}
