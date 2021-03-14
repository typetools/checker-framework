package org.checkerframework.framework.testchecker.lib;

public class UncheckedByteCode<CT> {
  public CT classTypeVariableField;
  public static Object nonFinalPublicField;

  public CT getCT() {
    return classTypeVariableField;
  }

  public <T> T identity(T t) {
    return t;
  }

  public int getInt(int i) {
    return i;
  }

  public Integer getInteger(Integer i) {
    return i;
  }

  public String getString(CharSequence charSequence) {
    return "";
  }

  public <I extends CharSequence> I getI(I i) {
    return i;
  }

  public Object getObject(Object o) {
    return o;
  }

  public static void unboundedWildcardParam(UncheckedByteCode<?> param) {}

  public static void upperboundedWildcardParam(UncheckedByteCode<? extends Object> param) {}

  public static void lowerboundedWildcardParam(UncheckedByteCode<? super Object> param) {}

  public static <F extends Number> void methodWithTypeVarBoundedByNumber(F param) {}
}
