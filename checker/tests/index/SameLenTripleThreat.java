import org.checkerframework.checker.index.qual.*;

public class SameLenTripleThreat {
  public void foo(String[] vars) {
    String[] qrets = new String[vars.length];
    String @SameLen("vars") [] y = qrets;
    String[] indices = new String[vars.length];
    String @SameLen("qrets") [] x = indices;
  }

  String[] indices;

  public void foo2(String... vars) {
    String[] qrets = new String[vars.length];
    indices = new String[vars.length];
    String[] indicesLocal = new String[vars.length];
    for (int i = 0; i < qrets.length; i++) {
      indices[i] = "hello";
      indicesLocal[i] = "hello";
    }
  }

  public void foo3(String... vars) {
    String[] qrets = new String[vars.length];
    String[] indicesLocal = new String[vars.length];
    indices = new String[vars.length];
    for (int i = 0; i < qrets.length; i++) {
      indices[i] = "hello";
      indicesLocal[i] = "hello";
    }
  }
}
