package org.checkerframework.afu.annotator.tests;

public class ExtImpl {
  class Top<X, Y> {}

  interface Iface<A, B> {}

  interface Iface2<C, D> {}

  interface Iface3 {}

  interface Iface4<T, S extends Iface4<T, S>> {}

  class C1 extends Top<Object, String> implements Iface<Integer, String> {}

  class C2 implements Iface<String, Object>, Iface2<Object, Float> {}

  class C3 {
    class Iface3 implements org.checkerframework.afu.annotator.tests.ExtImpl.Iface3 {}

    /*
     * the jaif file  says that the simple name of
     * the return type in JVM format is
     * LIface3;
     */
    org.checkerframework.afu.annotator.tests.ExtImpl.C3.Iface3 getI1() {
      return null;
    }

    /*
     * in this case, the jaif file uses the fully qualified name
     * for the return type
     * Lorg.checkerframework.afu.annotator.tests.ExtImpl.C3.Iface3;
     */
    org.checkerframework.afu.annotator.tests.ExtImpl.C3.Iface3 getI2() {
      return null;
    }

    /*
     * the jaif file uses the simple name of the return type
     * LC3$Iface3;
     */
    Iface3 getI3() {
      return null;
    }

    /*
     * in the jaif file, the return type is Iface3
     * (ambiguous: could be short for the interface
     * org.checkerframework.afu.annotator.tests.ExtImpl.Iface3)
     */
    Iface3 getI4() {
      return null;
    }
  }
}
