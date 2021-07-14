import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

class StandardQualClass {
  // :: error: foo
  public static String s = null;
}

@DefaultQualifier(Nullable.class)
class DefaultQualClass {
  public static String s = null;
}

record StandardQualRecord(String m) {
  // :: error: foo
  public static String s = null;
  StandardQualRecord {
    // :: error: foo
    m = null;
  }
}

@DefaultQualifier(Nullable.class)
record DefaultQualRecord(String m) {
  public static String s = null;
  DefaultQualRecord {
    m = null;
  }
}
