// Tests that stub files are printed correctly for fields with multiple levels of generic types.

import java.util.HashMap;
import java.util.Map;

public class DoubleGeneric {
  static Map<String, Map<String, String>> map10 = new HashMap<String, Map<String, String>>();
}
