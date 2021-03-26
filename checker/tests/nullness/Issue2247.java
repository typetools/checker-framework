// This is a test case for issue 2247:
// https://github.com/typetools/checker-framework/issues/2247

import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2247 {

  static @NonNull class DeclaredClass {}

  class ValidUseType {

    // :: error: (type.invalid.annotations.on.use)
    void test1(@Nullable DeclaredClass object) {}

    // :: error: (type.invalid.annotations.on.use)
    @Nullable DeclaredClass test2() {
      return null;
    }

    // :: error: (type.invalid.annotations.on.use)
    void test3(List<@Nullable DeclaredClass> param) {
      @Nullable DeclaredClass object = null;
      // :: error: (type.invalid.annotations.on.use)
      @Nullable DeclaredClass[] array = null;
    }

    // :: error: (type.invalid.annotations.on.use)
    <T extends @Nullable DeclaredClass> void test4(@NonNull T t) {}

    void test5(Map<String, DeclaredClass> map) {
      @Nullable DeclaredClass value = map.get("somekey");
      System.out.println(value);
      if (value != null) {
        @NonNull DeclaredClass nonnull = value;
      }
    }
  }
}
