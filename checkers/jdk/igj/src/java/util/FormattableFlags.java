package java.util;
import checkers.igj.quals.*;

@Immutable
public class FormattableFlags{
  public final static int LEFT_JUSTIFY = 1;
  public final static int UPPERCASE = 2;
  public final static int ALTERNATE = 4;

  protected FormattableFlags(@ReadOnly FormattableFlags this) {}
}
