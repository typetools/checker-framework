import checkers.nullness.quals.*;
import checkers.quals.DefaultQualifier;
import java.util.HashMap;
import java.util.regex.*;
import java.io.*;
import java.util.*;

@DefaultQualifier("NonNull")
public class Expressions {

  public static double[] returnDoubleArray() {
    return new double[] { 3.14, 2.7 };
  }

  public static void staticMembers() {
    Pattern.compile ("^>entry *()");
    System.out.println(Expressions.class);
    Expressions.class.getAnnotations();     // valid
  }

  private HashMap<String, String> map = new HashMap<String, String>();

  public void test() {
      @SuppressWarnings("nullness") String s = map.get("foo");

      Class<?> cl = Boolean.TYPE;

      List<?> foo = new LinkedList<Object>();
      foo.get(0).toString();   // default applies to wildcard extends

      Set set = new HashSet();
      for (@Nullable Object o : set) System.out.println();
  }

  void test2() {
      List<? extends @NonNull String> lst = new LinkedList<@NonNull String>();
      for (String s : lst) {
          s.length();
      }
  }

  <T extends @NonNull Object> void test3(T o) {
      o.getClass();     // valid
  }

  void test4(List<? extends @NonNull Object> o) {
      o.get(0).getClass();  // valid
  }

  void test5() {
      Comparable<Date> d = new Date();
  }

  void testIntersection() {
      java.util.Arrays.asList("m",1);
  }

  Object obj;
  public Expressions(Object obj) {
    this.obj = obj;
  }

  void testRawness(@Raw Object obj) {
      @SuppressWarnings("rawness")
      @NonRaw Object nonRaw = obj;
      this.obj = nonRaw;
  }
}
