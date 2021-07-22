import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java16-jdk-skip-test

public class NormalizingRecord {}

// TODO: Nest the rest of the file within NormalizingRecord when that doesn't crash the Checker
// Framework.

record NormalizingRecord1(@Nullable String s) {
  NormalizingRecord1(String s) {
    if (s.equals("")) {
      this.s = null;
    } else {
      this.s = s;
    }
  }
}

record NormalizingRecord2(String s) {
  NormalizingRecord2(@Nullable String s) {
    if (s == null) {
      s = "";
    }
    this.s = s;
  }
}

record NormalizingRecordIllegalConstructor1(String s) {
  NormalizingRecordIllegalConstructor1(@Nullable String s) {
    // :: error: (assignment)
    this.s = s;
  }
}

record NormalizingRecordIllegalConstructor2(@Nullable String s) {
  NormalizingRecordIllegalConstructor2(String s) {
    if (s.equals("")) {
      // The formal parametr type is @NonNull, so this assignment to it is illegal.
      // :: error: (assignment)
      s = null;
    }
    this.s = s;
  }
}

class Client {

  // :: error: (argument)
  NormalizingRecord1 nr1_1 = new NormalizingRecord1(null);
  NormalizingRecord1 nr1_2 = new NormalizingRecord1("");
  NormalizingRecord1 nr1_3 = new NormalizingRecord1("hello");
  @Nullable String nble = nr1_2.s();

  NormalizingRecord2 nr2_1 = new NormalizingRecord2(null);
  NormalizingRecord2 nr2_2 = new NormalizingRecord2("");
  NormalizingRecord2 nr2_3 = new NormalizingRecord2("hello");
  @NonNull String nn = nr2_1.s();
}
