import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class Bug {
  /** Actions that MultiVersionControl can perform. */
  static enum Action {
    /** Clone a repository. */
    CLONE,
    /** Show the working tree status. */
    STATUS,
    /** Pull changes from upstream. */
    PULL,
    /** List the known repositories. */
    LIST
  }

  private @MonotonicNonNull Action action;
  public static String home = System.getProperty("user.home");

  void other() {
    this.action = Action.LIST;
    expandTilde("");
  }

  /**
   * Replace "~" by the expansion of "$HOME".
   *
   * @param path the input path, which might contain "~"
   * @return path with "~" expanded
   */
  private static String expandTilde(String path) {
    return path.replaceFirst("^~", home);
  }

  public static void main(String[] args) {
    Bug b = new Bug();
    Bug.expandTilde("");
  }

  Bug() {
    parseArgs(new String[0]);
  }

  @EnsuresNonNull("action")
  public void parseArgs(@UnknownInitialization Bug this, String[] args) {
    String actionString = args[0];
    if ("checkout".startsWith(actionString)) {
      action = Action.CLONE;
    } else if ("clone".startsWith(actionString)) {
      action = Action.CLONE;
    } else if ("list".startsWith(actionString)) {
      action = Action.LIST;
    } else if ("pull".startsWith(actionString)) {
      action = Action.PULL;
    } else if ("status".startsWith(actionString)) {
      action = Action.STATUS;
    } else if ("update".startsWith(actionString)) {
      action = Action.PULL;
    } else {
      System.out.printf("Unrecognized action \"%s\"", actionString);
      System.exit(1);
    }
  }

  public static final @Nullable String VERSION =
      Bug.class.getPackage() != null ? Bug.class.getPackage().getImplementationVersion() : null;
}
