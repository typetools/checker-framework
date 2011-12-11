import java.util.*;
import java.io.*;

import checkers.nullness.quals.*;

public class IteratorEarlyExit {
  public static void main(String[] args) {
    List<String> array = new ArrayList<String>();
    String local = null;
    for (String str : array) {
      local = str;
      break;
    }
    //:: error: (dereference.of.nullable)
    System.out.println(local.length());
  }
}
