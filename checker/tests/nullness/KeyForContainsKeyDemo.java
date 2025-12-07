// Test case to demonstrate how containsKey works with dataflow analysis
// This shows why containsKey/put logic in KeyForTransfer is effective

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForContainsKeyDemo {

  void testContainsKey() {
    Map<@KeyFor({"map1"}) String, Integer> map1 = new HashMap<>();
    Map<@KeyFor({"map2"}) String, Integer> map2 = new HashMap<>();

    String x_key1 = "key1";
    String x_key2 = "key2";

    if (map1.containsKey(x_key1)) {
      map1.put(x_key1,1); // ok
    }
    map2.put(x_key2,1); //error
  }
}

