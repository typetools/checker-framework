import java.util.*;

import checkers.util.test.Critical;

abstract class ThrowCatch {

  void throwsUnencrypted() throws Exception {
    throw new Exception();
  }

  void throwsCritical() throws @Critical Exception {
    throw new @Critical Exception();
  }

  void catches() {
    try {
      throwsUnencrypted();
    } catch (Exception e) {
    }

    try {
      throwsUnencrypted();
    //:: (type.incompatible)
    } catch (@Critical Exception e) {
    }

    try {
      throwsCritical();
    } catch (Exception e) {
    }

    try {
      throwsCritical();
    } catch (@Critical Exception e) {
    }
  }

}
