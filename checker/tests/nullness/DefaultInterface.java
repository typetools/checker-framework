import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)
interface Foo {
  void foo(String a, String b);
}

@DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)
public class DefaultInterface {

  public void test() {

    @Nullable Foo foo = null;
  }
}
