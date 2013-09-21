import java.util.*;
import java.io.*;

import checkers.nullness.quals.*;

public class IteratorEarlyExit {
  public static void m1() {
    List<String> array = new ArrayList<String>();
    String local = null;
    for (String str : array) {
      local = str;
      break;
    }
    //:: error: (dereference.of.nullable)
    System.out.println(local.length());
  }

  public static void m2() {
    List<String> array = new ArrayList<String>();
    String local = null;
    for (String str : array) {
      local = str;
    }
    //:: error: (dereference.of.nullable)
    System.out.println(local.length());
  }

  public static void m3() {
    List<String> array = new ArrayList<String>();
    Object local = new Object();
    for (String str : array) {
      //:: error: (dereference.of.nullable)
      System.out.println(local.toString());
      // The next iteration might throw a NPE
      local = null;
    }
  }
}
