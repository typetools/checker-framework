package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class StrictMath{
  protected StrictMath() {}
  public final static double E = 2.718281828459045;
  public final static double PI = 3.141592653589793;
  public static strictfp double toRadians(double a1) { throw new RuntimeException("skeleton method"); }
  public static strictfp double toDegrees(double a1) { throw new RuntimeException("skeleton method"); }
  public static double rint(double a1) { throw new RuntimeException("skeleton method"); }
  public static int round(float a1) { throw new RuntimeException("skeleton method"); }
  public static long round(double a1) { throw new RuntimeException("skeleton method"); }
  public static double random() { throw new RuntimeException("skeleton method"); }
  public static int abs(int a1) { throw new RuntimeException("skeleton method"); }
  public static long abs(long a1) { throw new RuntimeException("skeleton method"); }
  public static float abs(float a1) { throw new RuntimeException("skeleton method"); }
  public static double abs(double a1) { throw new RuntimeException("skeleton method"); }
  public static int max(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static long max(long a1, long a2) { throw new RuntimeException("skeleton method"); }
  public static float max(float a1, float a2) { throw new RuntimeException("skeleton method"); }
  public static double max(double a1, double a2) { throw new RuntimeException("skeleton method"); }
  public static int min(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static long min(long a1, long a2) { throw new RuntimeException("skeleton method"); }
  public static float min(float a1, float a2) { throw new RuntimeException("skeleton method"); }
  public static double min(double a1, double a2) { throw new RuntimeException("skeleton method"); }
  public static double ulp(double a1) { throw new RuntimeException("skeleton method"); }
  public static float ulp(float a1) { throw new RuntimeException("skeleton method"); }
  public static double signum(double a1) { throw new RuntimeException("skeleton method"); }
  public static float signum(float a1) { throw new RuntimeException("skeleton method"); }
  public static double copySign(double a1, double a2) { throw new RuntimeException("skeleton method"); }
  public static float copySign(float a1, float a2) { throw new RuntimeException("skeleton method"); }
  public static int getExponent(float a1) { throw new RuntimeException("skeleton method"); }
  public static int getExponent(double a1) { throw new RuntimeException("skeleton method"); }
  public static double nextAfter(double a1, double a2) { throw new RuntimeException("skeleton method"); }
  public static float nextAfter(float a1, double a2) { throw new RuntimeException("skeleton method"); }
  public static double nextUp(double a1) { throw new RuntimeException("skeleton method"); }
  public static float nextUp(float a1) { throw new RuntimeException("skeleton method"); }
  public static double scalb(double a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static float scalb(float a1, int a2) { throw new RuntimeException("skeleton method"); }

  public static native double IEEEremainder(double a1, double a2);
  public static native double acos(double a1);
  public static native double asin(double a1);
  public static native double atan(double a1);
  public static native double atan2(double a1, double a2);
  public static native double cbrt(double a1);
  public static native double ceil(double a1);
  public static native double cos(double a1);
  public static native double cosh(double a1);
  public static native double exp(double a1);
  public static native double expm1(double a1);
  public static native double floor(double a1);
  public static native double hypot(double a1, double a2);
  public static native double log(double a1);
  public static native double log10(double a1);
  public static native double log1p(double a1);
  public static native double pow(double a1, double a2);
  public static native double sin(double a1);
  public static native double sinh(double a1);
  public static native double sqrt(double a1);
  public static native double tan(double a1);
  public static native double tanh(double a1);

}
