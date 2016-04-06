// Test case for issue 557:
// https://github.com/typetools/checker-framework/issues/557
// @below-java8-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

@SuppressWarnings("nullness")
class MyOpt<T> {
  static <S> MyOpt<S> of(S p) { return null; }
  static <S> MyOpt<S> empty() { return null; }
}

@SuppressWarnings("nullness")
class MyOpt2<T extends Object> {
  static <S extends Object> MyOpt2<S> of(S p) { return null; }
  static <S extends Object> MyOpt2<S> empty() { return null; }
}

@SuppressWarnings("nullness")
class MyOpt3<T extends @Nullable Object> {
  static <S extends @Nullable Object> MyOpt3<S> of(S p) { return null; }
  static <S extends @Nullable Object> MyOpt3<S> empty() { return null; }
}


class Issue557a {
    MyOpt<String> opt(boolean flag) {
      return flag ? MyOpt.of("Hello") : MyOpt.empty();
    }

    MyOpt<String> opt2() {
      return MyOpt.empty();
    }
}

class Issue557b {
    MyOpt2<String> opt(boolean flag) {
      return flag ? MyOpt2.of("Hello") : MyOpt2.empty();
    }

    MyOpt2<String> opt2() {
      return MyOpt2.empty();
    }
}

class Issue557c {
    MyOpt3<String> opt(boolean flag) {
      return flag ? MyOpt3.of("Hello") : MyOpt3.empty();
    }

    MyOpt3<String> opt2() {
      return MyOpt3.empty();
    }
}

class Issue557d {
    Optional<String> opt(boolean flag) {
      return flag ? Optional.of("Hello") : Optional.empty();
    }

    Optional<String> opt2() {
      return Optional.empty();
    }
}
