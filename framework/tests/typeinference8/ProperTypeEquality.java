// Test that two ProperTypes that stand for the same Java type are equal, even when they were
// created from different TypeMirrors.  The checking is done by
// org.checkerframework.framework.testchecker.typeinference8.ProperTypeChecker.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProperTypeEquality {

  String getFromList(List<String> list) {
    // The declared return type of List.get is the type variable E.
    return list.get(0);
  }

  List<String> emptyList() {
    // The declared return type of Collections.emptyList is the type variable T.
    return Collections.emptyList();
  }

  <T> T identity(T t) {
    return t;
  }

  Number useGenericMethod() {
    // The declared return type of identity is the type variable T.
    return identity(Integer.valueOf(1));
  }

  List<List<String>> nested() {
    List<List<String>> result = new ArrayList<>();
    result.add(Collections.<String>emptyList());
    return result;
  }
}
