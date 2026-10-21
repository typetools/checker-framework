// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/6872

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6872 {
  <T> void test(
      int i,
      Comparator<List<@NonNull T>> c1,
      Comparator<@Nullable Object> c2,
      Collection<List<@NonNull T>> nonNullLists,
      Collection<List<@Nullable T>> nullableLists,
      Collection<@Nullable List<@NonNull T>> listsOrNull) {
    Collections.min(
        nonNullLists,
        switch (i) {
          case 1 -> c1;
          default -> c2;
        });
    Collections.min(
        nonNullLists,
        switch (i) {
          case 1 -> c2;
          default -> c1;
        });

    // The lub of the switch arms is Comparator<? super List<@NonNull T>>.
    // :: error: (type.arguments.not.inferred)
    Collections.min(
        nullableLists,
        switch (i) {
          case 1 -> c1;
          default -> c2;
        });
    // :: error: (type.arguments.not.inferred)
    Collections.min(
        nullableLists,
        switch (i) {
          case 1 -> c2;
          default -> c1;
        });

    // c1 cannot compare null lists.
    // :: error: (type.arguments.not.inferred)
    Collections.min(
        listsOrNull,
        switch (i) {
          case 1 -> c1;
          default -> c2;
        });
    // :: error: (type.arguments.not.inferred)
    Collections.min(
        listsOrNull,
        switch (i) {
          case 1 -> c2;
          default -> c1;
        });
  }

  // The lub of the arms takes T's annotation from c1, so c2 is not a subtype of the lub.
  <T> void oneArmConstrainsT(
      int i, Comparator<List<@Nullable T>> c1, Comparator<Collection<@NonNull T>> c2) {
    Comparator<? super List<@Nullable T>> c =
        switch (i) {
          case 1 -> c1;
          // :: error: (switch.expression)
          default -> c2;
        };
    Comparator<? super List<@Nullable T>> c3 =
        switch (i) {
          // :: error: (switch.expression)
          case 1 -> c2;
          default -> c1;
        };
    // :: error: (assignment)
    Comparator<? super List<@Nullable T>> c4 = i == 1 ? c1 : c2;
  }

  <T> void bothArmsAcceptNullableT(
      int i, Comparator<List<@Nullable T>> c1, Comparator<Collection<@Nullable T>> c2) {
    Comparator<? super List<@Nullable T>> c =
        switch (i) {
          case 1 -> c1;
          default -> c2;
        };
  }

  // Neither arm annotates the T in List<T>, so the lub's T is unannotated.
  <T> void neitherArmAnnotatesT(
      int i, Comparator<@Nullable Object> c1, Comparator<Collection<@Nullable T>> c2) {
    Comparator<? super List<@Nullable T>> c =
        // TODO: This is a false positive.
        // :: error: (assignment)
        switch (i) {
          case 1 -> c1;
          // TODO: This is a false positive.
          // :: error: (switch.expression)
          default -> c2;
        };
  }

  <T> void neitherArmAnnotatesTInvalid(
      int i, Comparator<@Nullable Object> c1, Comparator<Collection<@NonNull T>> c2) {
    Comparator<? super List<@Nullable T>> c =
        // :: error: (assignment)
        switch (i) {
          case 1 -> c1;
          // :: error: (switch.expression)
          default -> c2;
        };
    // :: error: (assignment)
    Comparator<? super List<@Nullable T>> c3 = i == 1 ? c1 : c2;
  }
}
