import org.checkerframework.checker.nullness.qual.*;

public class PathJoins {

  public void testJoiningMultipleBranches() {
    Object intersect = null;
    if (false) {
      return;
    } else if (intersect == null) {
      return;
    } else {
      intersect = "m";
    }

    intersect.toString();
  }
}
