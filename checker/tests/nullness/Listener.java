import org.checkerframework.checker.nullness.qual.*;

public class Listener {

  @NonNull String f;

  public Listener() {
    Talker w = new Talker();
    // :: error: (argument)
    w.register(this);

    f = "abc";
  }

  public void callback() {
    System.out.println(f.toLowerCase());
  }

  public static class Talker {
    public void register(Listener s) {
      s.callback();
    }
  }

  public static void main(String[] args) {
    new Listener();
  }
}
