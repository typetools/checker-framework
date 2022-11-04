import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// @below-java17-jdk-skip-test
record RecordPurity(@Nullable String first, @Nullable String second) {
    public String checkNullnessOfFields() {
        // :: error: (dereference.of.nullable)
        return first.toString() + " " + second.toString();
    }

    public String checkNullnessOfAccessors() {
        // :: error: (dereference.of.nullable)
        return first().toString() + " " + second().toString();
    }

    public String checkPurityOfFields() {
        if (first == null || second == null) return "";
        else return "" + first.length() + second.length();
    }

    public static String checkPurityOfDefaultAccessor(RecordPurity r) {
        if (r.first() == null || r.second() == null) return "";
        else return "" + r.first().length() + " " + r.second().length();
    }

    public String checkPurityOfDefaultAccessorSelf() {
        if (first() == null || second() == null) return "";
        else return "" + first().length() + " " + second().length();
    }

    public String checkPurityOfDefaultAccessorSelf2() {
        if (first() == null) return "";
        if (second() == null) return "";

        return "" + first().length() + " " + second().length();
    }

    public String checkPurityOfDefaultAccessorSelfFirst() {
        if (first() == null) return "";
        else return "" + "".length() + first().length();
    }

    public String checkPurityOfDefaultAccessorSelfFirst2() {
        if (first() == null) return "";
        else return "" + "".length() + first().length() + first().length();
    }

    @Pure
    public @Nullable String pureMethod() {
        return "";
    }

    @Pure
    public @Nullable String pureMethod2() {
        return null;
    }

    public String checkPurityOfAccessor4() {
        if (pureMethod() == null) return "";
        else return "" + pureMethod().toString();
    }

    public String checkPurityOfAccessor5() {
        if (pureMethod() == null || pureMethod2() == null) return "";
        else return "" + pureMethod().length() + pureMethod2().length();
    }

    // An unrelated non-pure method of same name:
    public @Nullable String first(java.util.List<String> ss) {
        return ss.isEmpty() ? null : ss.get(0);
    }

    // An unrelated pure method of same name:
    @Pure
    public @Nullable String second(java.util.List<String> ss) {
        return ss.isEmpty() ? null : ss.get(1);
    }

    public String checkPurityOfImpureMethod() {
        java.util.List<String> ss = java.util.List.of();
        if (first(ss) == null) return "";
        else
            // :: error: (dereference.of.nullable)
            return "" + "".length() + first(ss).length();
    }

    public String checkPurityOfPureMethod() {
        java.util.List<String> ss = java.util.List.of();
        if (second(ss) == null) return "";
        else return "" + "".length() + second(ss).length();
    }
}
