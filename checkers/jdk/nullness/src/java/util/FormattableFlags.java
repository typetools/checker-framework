package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FormattableFlags{
  protected FormattableFlags() {}
  public final static int LEFT_JUSTIFY = 1;
  public final static int UPPERCASE = 2;
  public final static int ALTERNATE = 4;
}
