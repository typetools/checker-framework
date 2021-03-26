import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class AnnoBeforeModifier {

  // :: warning: (type.anno.before.modifier)
  @Unsigned public int i = 0;

  public @Unsigned int j = 0;

  // :: warning: (type.anno.before.modifier)
  public @Unsigned final int k = 0;

  @SuppressWarnings("foobar")
  @Unsigned public int l = 0;

  public @SuppressWarnings("foobar") @Unsigned int m = 0;

  @SuppressWarnings("foobar")
  @Unsigned public int n = 0;

  // TODO: :: warning: (type.anno.before.modifier)
  public @SuppressWarnings("foobar") @Unsigned final int o = 0;

  // :: warning: (type.anno.before.decl.anno) :: warning: (type.anno.before.modifier)
  public @Unsigned @SuppressWarnings("foobar") final int p = 0;

  public @SuppressWarnings("foobar") final @Unsigned int q = 0;

  @SuppressWarnings("foobar")
  public int r = 0;

  public @SuppressWarnings("foobar") int s = 0;

  public @SuppressWarnings("foobar") final int t = 0;

  // :: warning: (type.anno.before.modifier)
  @Unsigned public int iMethod() {
    return 0;
  }

  public @Unsigned int jMethod() {
    return 0;
  }

  // :: warning: (type.anno.before.modifier)
  public @Unsigned final int kMethod() {
    return 0;
  }

  @SuppressWarnings("foobar")
  @Unsigned public int lMethod() {
    return 0;
  }

  public @SuppressWarnings("foobar") @Unsigned int mMethod() {
    return 0;
  }

  @SuppressWarnings("foobar")
  @Unsigned public int nMethod() {
    return 0;
  }

  // TODO: :: warning: (type.anno.before.modifier)
  public @SuppressWarnings("foobar") @Unsigned final int oMethod() {
    return 0;
  }

  // :: warning: (type.anno.before.decl.anno) :: warning: (type.anno.before.modifier)
  public @Unsigned @SuppressWarnings("foobar") final int pMethod() {
    return 0;
  }

  public @SuppressWarnings("foobar") final @Unsigned int qMethod() {
    return 0;
  }

  @SuppressWarnings("foobar")
  public int rMethod() {
    return 0;
  }

  public @SuppressWarnings("foobar") int sMethod() {
    return 0;
  }

  public @SuppressWarnings("foobar") final int tMethod() {
    return 0;
  }

  // Use @NonNull rather than a signedness annotation to avoid errors.
  @NonNull public enum MyEnum {
    @NonNull CONSTANT
  }

  interface MyInterface {
    @NonNull String myMethod();
  }
}
