package org.checkerframework.afu.annotator.tests;

import java.util.Map;

public class InnerTypeResolution {
  Map.Entry method01(Map m) {
    return null;
  }

  Map.Entry method02(java.util.Map<String, Object> m) {
    return null;
  }
}
