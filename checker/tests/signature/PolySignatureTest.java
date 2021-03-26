import org.checkerframework.checker.signature.qual.*;

public class PolySignatureTest {

  @PolySignature String polyMethod(@PolySignature String arg) {
    return arg;
  }

  void m(@ClassGetName String s) {
    @ClassGetName String s1 = polyMethod(s);
    @ClassGetName String s2 = s.intern();
  }
}
