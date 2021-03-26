// Code to test that LUB of two AnnotatedTypeMirror does not crash.
// See Issue 643
// https://github.com/typetools/checker-framework/issues/643

import java.io.Serializable;
import java.util.List;

public class ConditionalExpressions {
  public static boolean flag = true;

  class TypeVarTypeVar {
    <T, S extends T> void foo1(T tExtendsNumber, S sExtendsT) {
      T o = flag ? tExtendsNumber : sExtendsT;
    }

    <T extends Number, S extends T> void foo2(T tExtendsNumber, S sExtendsT) {
      T o = flag ? tExtendsNumber : sExtendsT;
    }

    <T extends Number, S extends Integer> void foo3(T tExtendsNumber, S sExtendsInteger) {
      Number o3 = flag ? tExtendsNumber : sExtendsInteger;
    }

    <T extends Number, S extends CharSequence> void foo4(T tExtendsNumber, S sExtendsCharSequence) {
      Object o2 = flag ? tExtendsNumber : sExtendsCharSequence;
    }

    <T extends Long, S extends Integer> void foo5(T tExtendsLong, S sExtendsInt) {
      Number o = flag ? tExtendsLong : sExtendsInt;
    }
  }

  class ArrayTypes {
    void foo1(String string, String[] strings) {
      Serializable o2 = (flag ? string : strings);
    }

    void foo2(Integer[] integers, String[] strings) {
      Object[] o = (flag ? integers : strings);
    }

    <T extends Cloneable & Serializable> void foo3(T ts, Number[] numbers) {
      Cloneable o = flag ? ts : numbers;
    }

    <T> void foo4(T ts, Number[] numbers) {
      Object o = (flag ? ts : numbers);
    }
  }

  class Generics {
    void foo1(List<Long> listS, List<Integer> listI) {
      Number s = (flag ? listI : listS).get(0);
    }
  }
}
