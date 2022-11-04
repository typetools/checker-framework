import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java17-jdk-skip-test
record RecordPurityGeneric<A, B>(A a, B b) {
    public String checkNullnessOfFields() {
        // :: error: (dereference.of.nullable)
        return a.toString() + " " + b.toString();
    }

    public String checkNullnessOfAccessors() {
        // :: error: (dereference.of.nullable)
        return a().toString() + " " + b().toString();
    }

    public static String checkNullnessOfFields(
            RecordPurityGeneric<@Nullable String, @Nullable String> r) {
        // :: error: (dereference.of.nullable)
        return r.a.toString() + " " + r.b.toString();
    }

    public static String checkNullnessOfAccessors(
            RecordPurityGeneric<@Nullable String, @Nullable String> r) {
        // :: error: (dereference.of.nullable)
        return r.a().toString() + " " + r.b().toString();
    }

    public String checkPurityOfFields() {
        if (a == null || b == null) return "";
        else return a.toString() + " " + b.toString();
    }

    public String checkPurityOfFields(RecordPurityGeneric<@Nullable String, @Nullable String> r) {
        if (r.a == null || r.b == null) return "";
        else return r.a.toString() + " " + r.b.toString();
    }

    public static String checkPurityOfDefaultAccessor(
            RecordPurityGeneric<@Nullable String, @Nullable String> r) {
        if (r.a() == null || r.b() == null) return "";
        else return r.a().toString() + " " + r.b().toString();
    }

    public String checkPurityOfDefaultAccessorSelf() {
        if (a() == null || b() == null) return "";
        else return a().toString() + " " + b().toString();
    }
}
