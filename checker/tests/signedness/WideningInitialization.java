import org.checkerframework.checker.signedness.qual.Signed;

public class WideningInitialization {
  public int findLineNr(int pc, char startPC, char lineNr) {
    int ln = 0;
    for (int i = 0; i < 3; i++) {
      // :: error: (comparison.unsignedlhs)
      if (startPC <= pc) {
        ln = lineNr;
      } else {
        // :: error: (return)
        return ln;
      }
    }
    // :: error: (return)
    return ln;
  }

  public int findLineNr2(char lineNr) {
    int ln = 0;
    ln = lineNr;
    // :: error: (return)
    return ln;
  }

  public void findLineNr3a(char lineNr) {
    // :: error: (assignment)
    @Signed int ln = lineNr;
  }

  public int findLineNr3b(char lineNr) {
    int ln = lineNr;
    // :: error: (return)
    return ln;
  }

  public int findLineNr4(char lineNr) {
    // :: error: (return)
    return lineNr;
  }
}
