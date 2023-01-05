// This test checks that an anonymous class with a field
// doesn't cause a crash.

public class AnonymousClassWithField {

  public void scan(InterfaceTest foo) {
    // do nothing
  }

  public void test() {
    this.scan(
        new InterfaceTestExtension() {
          private String s1 = InterfaceTest.getAinferSibling1();

          @Override
          public void testX() {
            // :: warning: (argument)
            requireAinferSibling1(s1);
          }

          public void testY() {
            // :: warning: (argument)
            requireAinferSibling1(toaster);
          }
        });
  }
}
