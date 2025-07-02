package org.checkerframework.afu.scenelib.toys;

import java.util.HashMap;

public @interface SimplerAnnotation {
  BalanceEnum be();

  int height();

  int[] wrappedHeight();

  Class<? super HashMap<String, String>> favoriteClass();
}
