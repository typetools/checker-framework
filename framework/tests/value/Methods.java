import org.checkerframework.common.value.qual.*;

public class Methods {

  static int i = 3;
  static final int k = 3;

  public static void Length() {
    String a = "hello";
    @IntVal({5}) int b = a.length();

    String e = "hello";
    int f = 2;
    if (true) {
      f = 1;
      e = "world";
    }
    @IntVal({'e', 'l', 'o', 'r'}) char g = e.charAt(f);

    // :: error: (assignment)
    @IntVal({'l'}) char h = e.charAt(i);

    @IntVal({'l'}) char j = e.charAt(k);
  }

  public static void Boolean() {
    String a = "true";
    @BoolVal({true}) boolean b = Boolean.valueOf(a);
  }

  public static void Byte() {
    @IntVal({127}) byte a = Byte.MAX_VALUE;
    @IntVal({-128}) byte b = Byte.MIN_VALUE;

    String c = "59";
    int d = 10;
    @IntVal({59}) byte e = Byte.valueOf(c, d);
  }

  public static void Character() {
    @IntVal({'c'}) char a = Character.toLowerCase('C');

    // :: error: (assignment)
    @BoolVal({false}) boolean b = Character.isWhitespace('\t');
  }

  public static void Double() {
    @DoubleVal({Double.MAX_VALUE}) double a = Double.MAX_VALUE;
    String b = "59.32";
    @DoubleVal({59.32}) double c = Double.valueOf(b);
  }

  public static void Float() {
    @IntVal({Float.MIN_EXPONENT}) int a = Float.MIN_EXPONENT;
    String b = "59.32";
    @DoubleVal({59.32f}) float c = Float.valueOf(b);
  }

  public static void Integer() {
    @IntVal({Integer.SIZE}) int a = Integer.SIZE;
    String b = "0";
    @IntVal({0}) int c = Integer.valueOf(b);
  }

  public static void Long() {
    @IntVal({Long.MAX_VALUE}) long a = Long.MAX_VALUE;
    String b = "53";
    @IntVal({53L}) long c = Long.valueOf(53L);
  }

  public static void Short() {
    @IntVal({Short.MIN_VALUE}) short a = Short.MIN_VALUE;

    String b = "53";
    @IntVal({53}) short c = Short.valueOf(b);
  }

  public static void String() {

    @StringVal({"herro"}) String a = "hello".replace('l', 'r');
    // :: error: (assignment)
    @StringVal({"hello"}) String b = "hello".replace('l', 'r');
  }
}
