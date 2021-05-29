// Test case for Issue 1084
// https://github.com/typetools/checker-framework/issues/1084

import org.checkerframework.checker.nullness.qual.NonNull;

class MyOpt<T extends Object> {
  static <S> MyOpt<@NonNull S> empty() {
    throw new RuntimeException();
  }

  static <S> MyOpt<S> of(S p) {
    throw new RuntimeException();
  }
}

public class Issue1084 {
  MyOpt<Long> get() {
    return this.hashCode() > 0 ? MyOpt.of(5L) : MyOpt.empty();
  }

  MyOpt<byte[]> oba = MyOpt.empty();
}
