import java.io.File;
import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DaikonFail {

  public static @Nullable File generate_goals = null;
  public static File diff_file = new File("InvariantFormatTest.diffs");

  private boolean execute() {

    boolean result = performTest();

    if (generate_goals != null) {
      try {
        PrintStream out_fp = new PrintStream(generate_goals);
        out_fp.close();
      } catch (Exception e) {
        throw new RuntimeException();
      }
    } else {
      if (!result) {
        try {
          PrintStream diff_fp = new PrintStream(diff_file);
          diff_fp.close();
        } catch (Exception e) {
        }
        return false;
      }
    }
    return true;
  }

  private boolean performTest() {
    return false;
  }
}
