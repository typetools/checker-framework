// Tests that an annotations.xml item whose annotations the Nullness Checker ignores does not
// hide the annotated JDK's types for the item's element.

import java.util.Map;

public class IntellijIgnoredAnnotations {

  void mapGet(Map<String, String> map, String key) {
    // :: error: [dereference.of.nullable]
    map.get(key).toString();
  }

  String valueOfNull() {
    return String.valueOf((Object) null);
  }

  // The item for System.getProperty has an ignored annotation and a relevant one.
  int getPropertyLength() {
    return System.getProperty("user.dir").length();
  }
}
