/** BoundR and unbound constraints. */
interface UnboundR {
  void consume(/*1*/ UnboundR this, /*2*/ MyClass my, String s);
}

interface BoundR {
  void consume(/*4*/ BoundR this, String s);
}

interface SupplierR<R> {
  R supply();
}

class MyClass {

  void take(/*6*/ MyClass this, String s) {}

  void context(/*7*/ MyClass my) {
    /*8*/ UnboundR u1 = /*9*/ MyClass::take;
    // 2 <: 6 -- like an override
    // No relation to 1 or 2
    // No relationship or check for 8 / 9?
    // Need to check on this.

    u1.consume(my, "");
    // 7 <: 2
    // 8 <: 1

    /*10*/ BoundR b1 = /*11*/ my::take;
    // 7 <: 6 -- like an invocation
    // No Relationship for 10 / 11?

    b1.consume("");
    // 10 <: 4
  }
}

/** Constraints for implicit inner constraints and super. */
@SuppressWarnings("lock")
class OuterR {
  class Inner {
    Inner(/*1*/ OuterR OuterR.this) {}

    void context() {
      SupplierR<String> o = OuterR.super::toString;
    }
  }

  void context(/*2*/ OuterR this) {
    // This one is unbound and needs an OuterR as a param
    SupplierR</*3*/ Inner> f = /*4*/ Inner::new;
  }
}
