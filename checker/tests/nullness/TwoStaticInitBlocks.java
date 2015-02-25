import java.util.regex.*;

import org.checkerframework.checker.nullness.qual.Nullable;

// this is a test-case for initialization that covers multiple initializer blocks, field initializers and a few other things
class TwoStaticInitBlocks {
  
  String f2;
  String f1 = (f2 = "");
  
  {
    t = "";
    f1.toString();
    f2.toString();
  }

  final String ws_regexp;
  String t;
  String s;

  {
    ws_regexp = "hello";
    t.toString();
    //:: error: (dereference.of.nullable)
    s.toString();
  }

}

class B {
    static String f2;
    static String f1 = (f2 = "");
    
    static {
      t = "";
      f1.toString();
      f2.toString();
    }

    static final String ws_regexp;
    static String t;

    static {
      ws_regexp = "hello";
      t.toString();
    }
}
