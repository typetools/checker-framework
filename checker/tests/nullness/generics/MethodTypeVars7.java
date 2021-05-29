// Test case based on checker-framework discuss mailing list discussion
// "Generics problem with @Nullable method parameter" from May 16, 2014
// https://groups.google.com/d/msg/checker-framework-discuss/-gPGQ7mHjYI/YxCtjjBWx5cJ

import org.checkerframework.checker.nullness.qual.*;

abstract class MethodTypeVars7 {

  abstract <T> T val(@Nullable T value, T defaultValue);

  void tests(@Nullable String t1, @NonNull String t2) {
    @Nullable String s3 = val(t1, null);
  }

  <T> T validate(@Nullable T value, T defaultValue) {
    return value != null && !value.toString().isEmpty() ? value : defaultValue;
  }

  <T> T validateIf(@Nullable T value, T defaultValue) {
    if (value != null && !value.toString().isEmpty()) {
      return value;
    } else {
      return defaultValue;
    }
  }

  <T> T validate2(@Nullable T value, T defaultValue) {
    return value == null || value.toString().isEmpty() ? defaultValue : value;
  }

  <T> T validate3(@Nullable T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  <T> T validate4(@Nullable T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  <T> T validatefail(@Nullable T value, T defaultValue) {
    // :: error: (return)
    return ((value == null || !value.toString().isEmpty()) ? value : defaultValue);
  }

  <T> T validate2fail(@Nullable T value, T defaultValue) {
    // :: error: (return)
    return ((value != null && value.toString().isEmpty()) ? defaultValue : value);
  }

  <T> T validate3fail(@Nullable T value3, T defaultValue3) {
    // :: error: (return)
    return value3 == null ? value3 : defaultValue3;
  }

  <T> T validate4fail(@Nullable T value, T defaultValue) {
    // :: error: (return)
    return value != null ? defaultValue : value;
  }

  String test1(@Nullable String t1, @NonNull String t2) {
    @Nullable String s1 = validate(t1, null);
    @Nullable String s2 = validate(t2, null);
    @NonNull String s3 = validate(t1, "N/A");
    @NonNull String s4 = validate(t2, "N/A");
    return "[" + s1 + "\t" + s2 + "\t" + s3 + "\t" + s4 + "]";
  }

  String test2(@Nullable String t1, @NonNull String t2) {
    @Nullable String s1 = validate(t1, t1);
    @Nullable String s2 = validate(t2, t1);
    @NonNull String s3 = validate(t1, t2);
    @NonNull String s4 = validate(t2, t2);
    return "[" + s1 + "\t" + s2 + "\t" + s3 + "\t" + s4 + "]";
  }

  void main(String[] args) {
    System.out.println("test 1 " + test1("s_1", "s_2"));
    System.out.println("test 2 " + test2("s_1", "s_2"));
    System.out.println("test 1 " + test1(null, "s_2"));
    System.out.println("test 2 " + test2(null, "s_2"));
  }
}
