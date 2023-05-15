import org.checkerframework.checker.signedness.qual.Unsigned;

public class StringConcat {
  public String doConcat(@Unsigned int i, String s) {
    // :: error: (unsigned.concat)
    return s + i;
  }
}
