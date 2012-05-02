import java.util.*;
import java.io.*;
import checkers.nullness.quals.*;

public class FlowNonThis {

  @Nullable String c;

  public static void main(String[] args) {
    FlowNonThis t = new FlowNonThis();
    t.setup();
    System.out.println(t.c.length());
    t.erase();
    // TODO:
    // //:: error: (dereference.of.nullable)
    System.out.println(t.c.length());
  }

  public void setupThenErase() {
    setup();
    System.out.println(c.length());
    erase();
    // TODO:
    // //:: error: (dereference.of.nullable)
    System.out.println(c.length());
  }

  public void justErase() {
    //:: error: (dereference.of.nullable)
    System.out.println(c.length());
    erase();
    //:: error: (dereference.of.nullable)
    System.out.println(c.length());
  }

  /*@AssertNonNullAfter("c")*/
  public void setup() {
    c = "setup";
  }

  /*@ Assert Null After("c")*/ // <- no such annotation
  public void erase() {
    c = null;
  }

}
