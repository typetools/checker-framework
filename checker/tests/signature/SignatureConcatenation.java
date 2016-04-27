import org.checkerframework.checker.signature.qual.*;

import java.util.ArrayList;

public class SignatureConcatenation {

  @ClassGetSimpleName String m(@ClassGetSimpleName String arg1, @ClassGetSimpleName String arg2) {
    //:: error: (return.type.incompatible)
    return arg1 + arg2;
  }

}
