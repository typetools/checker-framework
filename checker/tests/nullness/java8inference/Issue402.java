// Test case that was submitted in Issue 402, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979

import java.util.Comparator;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

// Type argument inference does not infer the correct types.
// Once Issue 979 is fixed, this suppression should be removed.
@SuppressWarnings({"nullness", "keyfor"}) // Issue 979
public final class Issue402 {
  static final Comparator<Issue402> COMPARATOR =
      Comparator.comparing(Issue402::getStr1, Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(Issue402::getStr2, Comparator.nullsFirst(Comparator.naturalOrder()));

  @CheckForNull private final String str1;
  @CheckForNull private final String str2;

  Issue402(@Nullable final String str1, @Nullable final String str2) {
    this.str1 = str1;
    this.str2 = str2;
  }

  @CheckForNull
  String getStr1() {
    return this.str1;
  }

  @CheckForNull
  String getStr2() {
    return this.str2;
  }
}
