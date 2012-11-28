import checkers.nullness.quals.*;

public class PathJoins {

    public void testJoiningMultipleBranches() {
        Object intersect = null;
        if (false) {
          return;
        } else if (intersect==null) {
          return;
        } else {
            intersect = "m";
        }

        intersect.toString();
      }
}
