package fieldinvar;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.FieldInvariant;

public class NullnessFieldInvar {
  public class Super {
    public final @Nullable Object o;
    public @Nullable Object nonfinal = null;

    public Super(@Nullable Object o) {
      this.o = o;
    }
  }

  @FieldInvariant(field = "o", qualifier = NonNull.class)
  class Sub extends Super {
    public final @Nullable Object subO;

    public Sub(@NonNull Object o) {
      super(o);
      subO = null;
    }

    public Sub(@NonNull Object o, @Nullable Object subO) {
      super(o);
      this.subO = subO;
    }

    void test() {
      @NonNull Object x1 = this.o;
      @NonNull Object x2 = o;
      @NonNull Object x3 = super.o;
    }
  }

  class SubSub1 extends Sub {
    public SubSub1(@NonNull Object o) {
      super(o);
    }
  }

  @FieldInvariant(
      field = {"o", "subO"},
      qualifier = NonNull.class)
  class SubSub2 extends Sub {
    public SubSub2(@NonNull Object o) {
      super(o);
    }
  }

  class Use {
    void test(Super superO, Sub sub, SubSub1 subSub1, SubSub2 subSub2) {
      // :: error: (assignment.type.incompatible)
      @NonNull Object x1 = superO.o;
      @NonNull Object x2 = sub.o;
      @NonNull Object x3 = subSub1.o;

      // :: error: (assignment.type.incompatible)
      @NonNull Object x5 = sub.subO;
      // :: error: (assignment.type.incompatible)
      @NonNull Object x6 = subSub1.subO;
      @NonNull Object x7 = subSub2.subO;
    }

    <SP extends Super, SB extends Sub, SS1 extends SubSub1, SS2 extends SubSub2> void test2(
        SP superO, SB sub, SS1 subSub1, SS2 subSub2) {
      // :: error: (assignment.type.incompatible)
      @NonNull Object x1 = superO.o;
      @NonNull Object x2 = sub.o;
      @NonNull Object x3 = subSub1.o;

      // :: error: (assignment.type.incompatible)
      @NonNull Object x5 = sub.subO;
      // :: error: (assignment.type.incompatible)
      @NonNull Object x6 = subSub1.subO;
      @NonNull Object x7 = subSub2.subO;
    }
  }

  class SuperWithNonFinal {
    @Nullable Object nonfinal = null;
  }
  // nonfinal isn't final
  // :: error: (field.invariant.not.final)
  @FieldInvariant(field = "nonfinal", qualifier = NonNull.class)
  class SubSubInvalid extends SuperWithNonFinal {}

  // field is declared in this class
  // :: error: (field.invariant.not.found)
  @FieldInvariant(field = "field", qualifier = NonNull.class)
  class Invalid {
    final Object field = new Object();
  }

  @FieldInvariant(
      field = {"o", "subO"},
      qualifier = NonNull.class)
  class Shadowing extends SubSub2 {
    @Nullable Object o;
    @Nullable Object subO;

    void test() {
      // :: error: (assignment.type.incompatible)
      @NonNull Object x = o; // error
      // :: error: (assignment.type.incompatible)
      @NonNull Object x2 = subO; // error

      @NonNull Object x3 = super.o;
      @NonNull Object x4 = super.subO;
    }

    public Shadowing() {
      super("");
    }
  }

  // inherits: @FieldInvariant(field = {"o", "subO"}, qualifier = NonNull.class)
  class Inherits extends SubSub2 {
    @Nullable Object o;
    @Nullable Object subO;

    void test() {
      // :: error: (assignment.type.incompatible)
      @NonNull Object x = o; // error
      // :: error: (assignment.type.incompatible)
      @NonNull Object x2 = subO; // error

      @NonNull Object x3 = super.o;
      @NonNull Object x4 = super.subO;
    }

    public Inherits() {
      super("");
    }
  }

  class Super2 {}

  // :: error: (field.invariant.not.wellformed)
  @FieldInvariant(
      field = {},
      qualifier = NonNull.class)
  class Invalid1 extends Super2 {}
  // :: error: (field.invariant.not.wellformed)
  @FieldInvariant(
      field = {"a", "b"},
      qualifier = {NonNull.class, NonNull.class, NonNull.class})
  class Invalid2 extends Super2 {}

  // :: error: (field.invariant.not.found)
  @FieldInvariant(field = "x", qualifier = NonNull.class)
  class NoSuper {}

  class SuperManyFields {
    public final @Nullable Object field1 = null;
    public final @Nullable Object field2 = null;
    public final @Nullable Object field3 = null;
    public final @Nullable Object field4 = null;
  }

  @FieldInvariant(
      field = {"field1", "field2", "field3", "field4"},
      qualifier = NonNull.class)
  class SubManyFields extends SuperManyFields {
    void test() {
      field1.toString();
      field2.toString();
      field3.toString();
      field4.toString();
    }
  }
}
