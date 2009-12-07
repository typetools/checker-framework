import java.util.*;

import checkers.util.test.Encrypted;

abstract class ThrowCatch {

  void throwsUnencrypted() throws Exception {
    throw new Exception();
  }

  void throwsEncrypted() throws @Encrypted Exception {
    throw new @Encrypted Exception();
  }

  void catches() {
    try {
      throwsUnencrypted();
    } catch (Exception e) {
    }

    try {
      throwsUnencrypted();
    //:: (type.incompatible)
    } catch (@Encrypted Exception e) {
    }

    try {
      throwsEncrypted();
    } catch (Exception e) {
    }

    try {
      throwsEncrypted();
    } catch (@Encrypted Exception e) {
    }
  }

}
