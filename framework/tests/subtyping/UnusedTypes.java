import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.Unused;

// This test case is quite meaningless, as it's not run with the
// Nullness Checker. See nullness/UnusedNullness.java instead.
public class UnusedTypes {

  @SubtypeOf({})
  @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
  public @interface Prototype {}

  @Unused(when = Prototype.class)
  public Object ppt;

  protected @Prototype UnusedTypes() {
    // It should be legal to initialize an unused field to null in the
    // constructor.
    this.ppt = null;
  }
}
