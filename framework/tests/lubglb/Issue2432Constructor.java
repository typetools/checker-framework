// Test case for issue 2432, constructor part:
// https://github.com/typetools/checker-framework/issues/2432

import org.checkerframework.framework.testchecker.lubglb.quals.*;

class Issue2432C {

  // reason for suppressing:
  // super.invocation: Object is @LubglbA by default and it is unreasonable to change jdk stub
  // just because of this
  // inconsistent.constructor.type: the qualifier on returning type is expected not to be top
  @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
  @PolyLubglb
  Issue2432C(@PolyLubglb Object dummy) {}

  @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
  @PolyLubglb
  Issue2432C(@PolyLubglb Object dummy1, @PolyLubglb Object dummy2) {}

  // class for test cases using type parameter
  static class TypeParamClass<T> {

    // @PolyLubglb on T shouldn't be in the poly resolving process
    @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
    @PolyLubglb
    TypeParamClass(@PolyLubglb Object dummy, T t) {}

    // 2 poly param for testing lub
    @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
    @PolyLubglb
    TypeParamClass(@PolyLubglb Object dummy1, @PolyLubglb Object dummy2, T t) {}
  }

  // class for test cases using type parameter
  class ReceiverClass {

    // if the qualifier on receiver is @PolyLubglb, it should not be involved in poly resolve
    // process
    @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
    @PolyLubglb
    ReceiverClass(Issue2432C Issue2432C.this, @PolyLubglb Object dummy) {}

    // 2 poly param for testing lub
    @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
    @PolyLubglb
    ReceiverClass(
        Issue2432C Issue2432C.this, @PolyLubglb Object dummy1, @PolyLubglb Object dummy2) {}
  }

  void invokeConstructors(@LubglbA Object top, @LubglbF Object bottom, @PolyLubglb Object poly) {
    // :: error: (assignment)
    @LubglbF Issue2432C bottomOuter = new Issue2432C(top);
    @LubglbA Issue2432C topOuter = new Issue2432C(top);

    // lub test
    @LubglbA Issue2432C bottomOuter2 = new Issue2432C(top, bottom);
    // :: error: (assignment)
    @LubglbB Issue2432C bottomOuter3 = new Issue2432C(top, bottom);

    @LubglbF Issue2432C bottomOuter4 = new Issue2432C(bottom, bottom);
  }

  // invoke constructors with a receiver to test poly resolving
  // note: seems CF already works well on these before changes
  void invokeReceiverConstructors(
      @LubglbA Issue2432C topOuter,
      @PolyLubglb Issue2432C polyOuter,
      @LubglbF Object bottom,
      @LubglbA Object top) {
    Issue2432C.@LubglbF ReceiverClass ref1 = polyOuter.new ReceiverClass(bottom);
    // :: error: (assignment)
    Issue2432C.@LubglbB ReceiverClass ref2 = polyOuter.new ReceiverClass(top);

    // lub tests
    Issue2432C.@LubglbA ReceiverClass ref3 = polyOuter.new ReceiverClass(top, bottom);
    // :: error: (assignment)
    Issue2432C.@LubglbB ReceiverClass ref4 = polyOuter.new ReceiverClass(top, bottom);

    Issue2432C.@LubglbF ReceiverClass ref5 = polyOuter.new ReceiverClass(bottom, bottom);
  }

  // invoke constructors with a type parameter to test poly resolving
  void invokeTypeVarConstructors(
      @LubglbA Object top, @LubglbF Object bottom, @PolyLubglb Object poly) {
    @LubglbF TypeParamClass<@PolyLubglb Object> ref1 = new TypeParamClass<>(bottom, poly);
    // :: error: (assignment)
    @LubglbB TypeParamClass<@PolyLubglb Object> ref2 = new TypeParamClass<>(top, poly);

    // lub tests
    @LubglbA TypeParamClass<@PolyLubglb Object> ref3 = new TypeParamClass<>(bottom, top, poly);
    // :: error: (assignment)
    @LubglbB TypeParamClass<@PolyLubglb Object> ref4 = new TypeParamClass<>(bottom, top, poly);

    @LubglbF TypeParamClass<@PolyLubglb Object> ref5 = new TypeParamClass<>(bottom, bottom, poly);
  }
}
