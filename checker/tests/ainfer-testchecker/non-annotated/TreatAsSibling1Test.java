// A simple test that the @AinferTreatAsSibling1 annotation works as intended.
// This test doesn't actually test inference: it's a test for the AinferTestChecker.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTreatAsSibling1;

public class TreatAsSibling1Test {
  public void test(@AinferTreatAsSibling1 Object y) {
    @AinferSibling1 Object x = y;
  }
}
