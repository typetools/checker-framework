package org.checkerframework.afu.scenelib.toys;

@ValuesAnnotation(
    B = -128,
    S = -32768,
    I = -2147483648,
    J = -3141592653589793238L,
    F = 0.1e-5f,
    D = 9.8e99,
    Z = true,
    C = '\'',
    Ltok = java.util.Map.Entry.class,
    string = "\"yfwq\" yfwq \'\n\t\\",
    arrayI = {1, 2},
    arrayI2 = {},
    balEnum = BalanceEnum.BALANCED,
    subann = @SubAnnotation({3, 4}),
    arraySubann = {@SubAnnotation({}), @SubAnnotation({5})})
public @interface ValuesAnnotation {
  byte B();

  short S();

  int I();

  long J();

  float F();

  double D();

  boolean Z();

  char C();

  Class<?> Ltok();

  String string();

  int[] arrayI();

  int[] arrayI2();

  BalanceEnum balEnum();

  SubAnnotation subann();

  SubAnnotation[] arraySubann();
}
