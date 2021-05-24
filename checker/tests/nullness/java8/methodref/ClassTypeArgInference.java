import org.checkerframework.checker.nullness.qual.Nullable;

public class ClassTypeArgInference {
  public static void main(String[] args) {
    Gen<String> o = new Gen<>("");
    // :: error: (methodref.param)
    Factory f = Gen::make;
    // :: error: (methodref.param)
    Factory f2 = Gen<String>::make;
    // :: error: (methodref.receiver) :: error: (methodref.return)
    Factory f3 = Gen<@Nullable String>::make;
    f2.make(o, null).toString();
  }

  static class Gen<G> {
    G field;

    Gen(G g) {
      field = g;
    }

    public G getField() {
      return field;
    }

    G make(G g) {
      return g;
    }

    Gen<G> id() {
      return this;
    }
  }

  interface Factory {
    String make(Gen<String> g, @Nullable String t);
  }
}
