import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.AssertMethod;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class AssertMethodTest {
  @interface Anno {
    Class<?> value();
  }

  @AssertMethod
  @SideEffectFree
  void assertMethod(boolean b) {
    if (!b) {
      throw new RuntimeException();
    }
  }

  void test1(@Nullable Object o) {
    assertMethod(o != null);
    o.toString();
  }

  @Nullable Object getO() {
    return null;
  }

  void test2() {
    assertMethod(getO() != null);
    // :: error: dereference.of.nullable
    getO().toString(); // error
  }

  @Pure
  @Nullable Object getPureO() {
    return "";
  }

  void test3() {
    assertMethod(getPureO() != null);
    getPureO().toString();
  }

  @Nullable Object field = null;

  void test4() {
    assertMethod(field != null);
    field.toString();
  }

  String getError() {
    field = null;
    return "error";
  }

  @AssertMethod(value = RuntimeException.class, parameter = 2)
  @SideEffectFree
  void assertMethod(Object p, boolean b, Object error) {
    if (!b) {
      throw new RuntimeException();
    }
  }

  void test5() {
    assertMethod(getError(), field != null, getError());
    // :: error: dereference.of.nullable
    field.toString(); // error
  }

  void test5b() {
    assertMethod(getError(), field != null, "");
    field.toString();
  }

  @AssertMethod(isAssertFalse = true)
  @SideEffectFree
  void assertFalse(boolean b) {
    if (b) {
      throw new RuntimeException();
    }
  }

  void test6() {
    assertFalse(field == null);
    field.toString();
  }
}
