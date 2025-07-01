package annotations.tests;

import java.lang.annotation.*;
import java.util.*;

@interface AClass {}

@Target(ElementType.TYPE_USE)
@interface A00 {}

@Target(ElementType.TYPE_USE)
@interface A01 {}

@Target(ElementType.TYPE_USE)
@interface A02 {}

@Target(ElementType.TYPE_USE)
@interface A04 {}

@Target(ElementType.TYPE_USE)
@interface A05 {}

@Target({ElementType.TYPE_USE, ElementType.PARAMETER})
@interface A06 {}

@Target(ElementType.TYPE_USE)
@interface A08 {}

@Target(ElementType.TYPE_USE)
@interface A09 {}

@Target({ElementType.TYPE_USE, ElementType.METHOD})
@interface A0A {}

@Target(ElementType.TYPE_USE)
@interface A0AT {}

@Target(ElementType.TYPE_USE)
@interface A0B {}

@Target({ElementType.TYPE_USE, ElementType.PARAMETER})
@interface A0C {}

@Target(ElementType.TYPE_USE)
@interface A0D {}

@Target({ElementType.TYPE_USE, ElementType.FIELD})
@interface A0E {}

@Target({ElementType.TYPE_USE})
@interface A21 {}

@Target(ElementType.TYPE_USE)
@interface A20 {}

@Target(ElementType.TYPE_USE)
@interface A22 {}

@Target(ElementType.TYPE_USE)
@interface A23 {}

@Target(ElementType.TYPE_USE)
@interface A24 {}

@interface A2B {}

@Target(ElementType.TYPE_USE)
@interface A2C {}

@Target(ElementType.TYPE_USE)
@interface A2D {}

@Target(ElementType.TYPE_USE)
@interface A2E {}

@Target(ElementType.TYPE_USE)
@interface A2F {}

@interface A10 {}

@interface A11 {}

@interface A12 {}

@interface A13 {}

@interface CClass {}

@Target(ElementType.TYPE_USE)
@interface C00 {}

@Target(ElementType.TYPE_USE)
@interface C01 {}

@Target(ElementType.TYPE_USE)
@interface C02 {}

@Target(ElementType.TYPE_USE)
@interface C04 {}

@Target(ElementType.TYPE_USE)
@interface C05 {}

@Target({ElementType.TYPE_USE, ElementType.PARAMETER})
@interface C06 {}

@Target(ElementType.TYPE_USE)
@interface C08 {}

@Target(ElementType.TYPE_USE)
@interface C09 {}

@Target({ElementType.TYPE_USE, ElementType.METHOD})
@interface C0A {}

@Target(ElementType.TYPE_USE)
@interface C0AT {}

@Target(ElementType.TYPE_USE)
@interface C0B {}

@Target({ElementType.TYPE_USE, ElementType.PARAMETER})
@interface C0C {}

@Target(ElementType.TYPE_USE)
@interface C0D {}

@Target({ElementType.TYPE_USE, ElementType.FIELD})
@interface C0E {}

@Target(ElementType.TYPE_USE)
@interface C20 {}

@Target(ElementType.TYPE_USE)
@interface C21 {}

@Target(ElementType.TYPE_USE)
@interface C22 {}

@Target(ElementType.TYPE_USE)
@interface C23 {}

@Target(ElementType.TYPE_USE)
@interface C24 {}

@interface C2B {}

@Target(ElementType.TYPE_USE)
@interface C2C {}

@Target(ElementType.TYPE_USE)
@interface C2D {}

@Target(ElementType.TYPE_USE)
@interface C2E {}

@Target(ElementType.TYPE_USE)
@interface C2F {}

@interface C10 {}

@interface C11 {}

@interface C12 {}

@interface C13 {}

public @AClass @CClass class AnnotationTest<
    Foo extends /*A10*/ /*C10*/ Comparable</*A11*//*C11*/ Integer>> {
  class Outer {
    class Inner<Baz> {
      int baz(Baz o) {
        return o.hashCode() ^ this.hashCode();
      }
    }
  }

  @A0E @C0E Iterable<@A21 @C21 String @A20 @C20 []> field;
  @A24 @C24 Outer.@A22 @C22 Inner<@A23 @C23 String> inner;

  @A2B @C2B
  Map.@A2C @C2C Entry<@A2D @C2D Integer, @A2E @C2E ? extends @A2F @C2F CharSequence> entry;

  // TODO: crash when A12, C12, A13, or C13 are annotations!
  <Bar extends /*A12*/ /*C12*/ Comparable</*A13*//*C13*/ Integer>>
      @A0A @C0A @A0AT @C0AT HashSet<@A0B @C0B Integer> doSomething(
          @A06 @C06 AnnotationTest<Foo> this, @A0C @C0C Set<@A0D @C0D Integer> param) {
    @A08
    @C08
    HashSet<@A09 @C09 Integer> local;
    if (param instanceof @A02 @C02 HashSet) local = (@A00 @C00 HashSet<@A01 @C01 Integer>) param;
    else local = new @A04 @C04 HashSet<@A05 @C05 Integer>();
    return local;
  }
}
