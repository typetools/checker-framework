import org.checkerframework.checker.nullness.qual.NonNull;

class PairUsage {
    public void makePairs() {
        PairRecord a = new PairRecord("key", "value");
        PairRecord b = new PairRecord(null);
        // :: error: (assignment)
        @NonNull Object o = a.value();
        PairRecord p = new PairRecord("key", null);
    }

    public void makeStubbed() {
        RecordStubbed r = new RecordStubbed("a", "b", 7);
        RecordStubbed r1 = new RecordStubbed("a", "b", null);
        // :: error: (argument)
        RecordStubbed r2 = new RecordStubbed((String) null, "b", null);
        // :: error: (argument)
        RecordStubbed r3 = new RecordStubbed("a", null, null);
        @NonNull Object o = r.nxx();
        @NonNull Object o2 = r.nsxx();
        // :: error: (assignment)
        @NonNull Object o3 = r.xnn();
    }
}
