import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.*;

import java.util.*;

public class AssertAfterChecked {

  class InitField {
    @Nullable Object f;

    @EnsuresNonNull("f")
    void init() {
      f = new Object();
    }

    //:: error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("f") void initBad() {
    }

    void testInit() {
      init();
      f.toString();
    }
  }

  static class InitStaticField {
    static @Nullable Object f;

    @EnsuresNonNull("f")
    void init() {
      f = new Object();
    }

    @EnsuresNonNull("f")
    void init2() {
      InitStaticField.f = new Object();
    }

    //:: error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("f") void initBad() {
    }

    void testInit() {
      init();
      f.toString();
    }

    @EnsuresNonNull("InitStaticField.f")
    void initE() {
      f = new Object();
    }

    @EnsuresNonNull("InitStaticField.f")
    void initE2() {
      InitStaticField.f = new Object();
    }

    //:: error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("InitStaticField.f") void initBadE() {
    }

    void testInitE() {
      initE();
      // TODO: we need to also support the unqualified static field access?
      // f.toString();
    }

    void testInitE2() {
      initE();
      InitStaticField.f.toString();
    }
  }

  class TestParams {
    //:: error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("get(#1)") void init(final TestParams p) {

    }

    @org.checkerframework.dataflow.qual.Pure
    @Nullable Object get(Object o) {
      return null;
    }


    void testInit1() {
      init(this);
      get(this).toString();
    }

    void testInit1b() {
      init(this);
      // TODO: the explicit this does not work :-((
      // this.get(this).toString();
    }

    void testInit2(TestParams p) {
      init(p);
      get(p).toString();
    }

    void testInit3(TestParams p) {
      p.init(this);
      p.get(this).toString();
    }

    void testInit4(TestParams p) {
      p.init(this);
      //:: error: (dereference.of.nullable)
      this.get(this).toString();
    }

  }

  class WithReturn {
    @Nullable Object f;

    @EnsuresNonNull("f")
    int init1() {
      f = new Object();
      return 0;
    }

    @EnsuresNonNull("f")
    int init2() {
      if (5==5) {
        f = new Object();
        return 0;
      } else {
        f = new Object();
        return 1;
      }
    }

    //:: error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("f") int initBad1() {
      return 0;
    }

    //:: error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("f") int initBad2() {
      if (5==5) {
        return 0;
      } else {
        f = new Object();
        return 1;
      }
    }

    void testInit() {
      init1();
      f.toString();
    }
  }


}
