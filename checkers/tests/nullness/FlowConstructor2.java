// @skip-test
public class FlowConstructor2 {
  String f;

  public FlowConstructor2() {
    //:: error: (dereference.of.nullable)
    f.hashCode();
  }

}
