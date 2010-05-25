import checkers.nullness.quals.*;

import java.util.*;

class RawAndReferences {

  static void method(Map<String,String> map) {
      for (String decl : sortedKeySet(map)) {
          @NonRaw String s = decl;
      }
  }

  static <K> Collection<K> sortedKeySet(Map<K,?> m) {
      throw new RuntimeException();
  }
}

