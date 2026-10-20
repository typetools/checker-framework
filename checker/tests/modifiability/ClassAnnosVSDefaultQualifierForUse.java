import java.util.ArrayList;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeGrowable;
import org.checkerframework.checker.modifiability.qual.UnmodifiableParam;
import org.checkerframework.framework.qual.DefaultQualifierForUse;

public class ClassAnnosVSDefaultQualifierForUse {

  // write annotation on class declaration.
  static @Growable class ModList<String> extends ArrayList<String> {}

  // :: error: [annotations.on.use]
  @MaybeGrowable ModList<String> list = new ModList<>();

  void test1(ModList<String> tests) {
    tests.add("a");
  }

  // :: error: [annotations.on.use]
  void foo(@UnmodifiableParam ModList<String> list) {}

  // use @DefaultQualifierForUse
  @DefaultQualifierForUse(Growable.class)
  static class DefaultList<String> extends ArrayList<String> {}

  @MaybeGrowable DefaultList<String> list2 = new DefaultList<>();

  void test2(DefaultList<String> tests) {
    tests.add("a");
  }

  void foo(@UnmodifiableParam DefaultList<String> list) {}
}
