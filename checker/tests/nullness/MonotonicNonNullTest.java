import org.checkerframework.checker.nullness.qual.*;

public final class MonotonicNonNullTest {

  public static @MonotonicNonNull Boolean new_decl_format = null;

  static final class SerialFormat {

    public boolean new_decl_format = false;

    @RequiresNonNull("MonotonicNonNullTest.new_decl_format")
    public SerialFormat() {
      this.new_decl_format = MonotonicNonNullTest.new_decl_format;
    }
  }
}
