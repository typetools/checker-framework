import org.checkerframework.checker.nullness.qual.NonNull;

class PairUsage {
  public void makePairs() {
    PairRecord a = new PairRecord("key", "value");
    PairRecord b = new PairRecord(null);
    // :: error: (assignment)
    @NonNull Object o = a.value();
    PairRecord p = new PairRecord("key", null);
  }
}
