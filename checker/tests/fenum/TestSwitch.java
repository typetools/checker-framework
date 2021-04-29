import org.checkerframework.checker.fenum.qual.Fenum;

public class TestSwitch {
  void m() {
    @SuppressWarnings("fenum:assignment")
    @Fenum("TEST") final int annotated = 3;

    @SuppressWarnings("fenum:assignment")
    @Fenum("TEST") final int annotated2 = 6;

    int plain = 9; // FenumUnqualified

    switch (plain) {
        // :: error: (switch)
      case annotated:
      default:
    }

    // un-annotated still working
    switch (plain) {
      case 1:
      case 2:
      default:
    }

    switch (annotated) {
        // :: error: (switch)
      case 45:
      default:
    }

    // annotated working
    switch (annotated) {
      case annotated2:
      default:
    }
  }
}
