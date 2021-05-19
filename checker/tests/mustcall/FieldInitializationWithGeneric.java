// based on a false positive I found in Zookeeper

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class FieldInitializationWithGeneric {
  private Set<String> activeObservers =
      Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
}
