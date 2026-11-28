// Tests a throw statement whose expression has an intersection type.

import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.UI;

public class ThrowIntersection {
  @PolyUIType
  interface I {}

  @PolyUIType
  class PolyUIException extends Exception {}

  void throwUI(Object x) throws PolyUIException {
    // :: error: [throw]
    throw (@UI PolyUIException & I) x;
  }

  void throwAlwaysSafe(Object x) throws PolyUIException {
    throw (@AlwaysSafe PolyUIException & I) x;
  }
}
