@interface A1 {
  String[] value() default {};
}

@interface A2 {
  String[] value();
}

public class Annotation {
  @A1({"a", "b"})
  void m1() {}

  @A1(value = {"a", "b"})
  void m2() {}

  @A2({"a", "b"})
  void m3() {}

  @A2(value = {"a", "b"})
  void m4() {}
}
