import java.util.Map;

public class MemberSelectTypes<T extends java.lang.Object & java.lang.Comparable>
    extends java.lang.Object implements java.io.Serializable {

  class Inner {
    void m(MemberSelectTypes<T>.Inner this) {}
  }

  static class StaticInner {
    void m(MemberSelectTypes.StaticInner this) {}
  }

  java.lang.Object o;
  java.util.Map.Entry<java.lang.String, java.util.Map.Entry<Object, Object>> m1;
  Map.Entry<java.lang.String, Map.Entry<Object, Object>> m2;
  Map<MemberSelectTypes.Inner, MemberSelectTypes.StaticInner> m3;
  Map.Entry<Map.Entry<Map.Entry<Map.Entry<Object, Object>, Object>, Object>, Object> m4;
  MemberSelectTypes.Inner i;
  MemberSelectTypes.StaticInner s;

  java.lang.Object m(
      java.lang.Object o, MemberSelectTypes.Inner i, MemberSelectTypes.StaticInner s) {
    java.lang.Object o2 = (java.lang.Object) o;
    MemberSelectTypes.Inner i2 = (MemberSelectTypes.Inner) i;
    MemberSelectTypes.StaticInner s2 = (MemberSelectTypes.StaticInner) s;
    o2 = new java.lang.Object();
    i2 = new MemberSelectTypes.Inner();
    s2 = new MemberSelectTypes.StaticInner();
    java.lang.Object[] os = new java.lang.Object[1];
    MemberSelectTypes.Inner[] is = new MemberSelectTypes.Inner[1];
    MemberSelectTypes.StaticInner[] ss = new MemberSelectTypes.StaticInner[1];
    boolean b = o instanceof java.lang.Object;
    b = o instanceof MemberSelectTypes.Inner;
    b = o instanceof MemberSelectTypes.StaticInner;
    return o2;
  }
}
