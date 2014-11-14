// Test case for Issue 355:
// https://code.google.com/p/checker-framework/issues/detail?id=355

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

class Issue355 {
    static <T extends @Nullable Object> @NonNull T checkNotNull(@Nullable T sample) {
        throw new RuntimeException();
    }

    void m(List<String> xs) {
        for (String x : checkNotNull(xs)) {
        }
    }
}

class Issue355b {
    static <T> T checkNotNull(T sample) {
      throw new RuntimeException();
    }

    void m(List<String> xs) {
        for (Object x : checkNotNull(xs)) {
        }
    }
}

class Issue355c {
    static <T> T checkNotNull(@NonNull T sample) {
      throw new RuntimeException();
    }

    void m(List<String> xs) {
      for (Object x : checkNotNull(xs)) {
      }
    }
  }

class Issue355d {
    static <T> @Nullable T checkNotNull(@NonNull T sample) {
      throw new RuntimeException();
    }

    void m(List<String> xs) {
      //:: error: (iterating.over.nullable)
      for (Object x : checkNotNull(xs)) {
      }
    }
  }

class Issue355e {
    static <T> @NonNull T checkNotNull(@NonNull T sample) {
      throw new RuntimeException();
    }

    void m(@Nullable List<String> xs) {
      //:: error: (argument.type.incompatible)
      for (Object x : checkNotNull(xs)) {
      }
    }
  }

