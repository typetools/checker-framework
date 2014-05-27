package java.util;
import org.checkerframework.checker.igj.qual.*;

@Immutable
public class FormattableFlags {
  public final static int LEFT_JUSTIFY = 1;
  public final static int UPPERCASE = 2;
  public final static int ALTERNATE = 4;

  private FormattableFlags() {}
}
