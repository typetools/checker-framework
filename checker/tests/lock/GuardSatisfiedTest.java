import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.GuardedBy;

public class GuardSatisfiedTest {
    void testMethodCall346436(@GuardSatisfied GuardSatisfiedTest this, @GuardedBy("lock1") Object o, @GuardedBy("lock2") Object p, @GuardSatisfied Object q) {
        methodToCall4();
    }


    void testGuardSatisfiedIndexMatching(@GuardSatisfied GuardSatisfiedTest this, @GuardSatisfied(1) Object o, @GuardSatisfied(2) Object p, @GuardSatisfied Object q) {
      methodToCall1(o, o);
      methodToCall1(p, p);
      //:: error: (guardsatisfied.parameters.must.match)
      methodToCall1(o, p);
      //:: error: (guardsatisfied.parameters.must.match)
      methodToCall1(p, o);
   }

   // Test defaulting of parameters - they must default to @GuardedBy({}), not @GuardSatisfied
   void testDefaulting(Object o) {
       @GuardSatisfied Object p = new Object();
       //:: error: (assignment.type.incompatible)
       o = p; // Must assign in this direction to test the defaulting because assigning a RHS of @GuardedBy({}) to a LHS @GuardSatisfied is legal.
       @GuardedBy({}) Object q = o;
   }

   void testMethodCall(@GuardSatisfied GuardSatisfiedTest this, @GuardedBy("lock1") Object o, @GuardedBy("lock2") Object p, @GuardSatisfied Object q) {
       // Test matching parameters

       //:: error: (contracts.precondition.not.satisfied.field)
       methodToCall1(o, o);
       //:: error: (contracts.precondition.not.satisfied.field) :: error: (guardsatisfied.parameters.must.match)
       methodToCall1(o, p);
       //:: error: (contracts.precondition.not.satisfied.field)
       methodToCall1(p, p);
       synchronized(lock2) {
           //:: error: (contracts.precondition.not.satisfied.field)
           methodToCall1(o, o);
           //:: error: (guardsatisfied.parameters.must.match) :: error: (contracts.precondition.not.satisfied.field)
           methodToCall1(o, p);
           methodToCall1(p, p);
           synchronized(lock1) {
               methodToCall1(o, o);
               //:: error: (guardsatisfied.parameters.must.match)
               methodToCall1(o, p);
               methodToCall1(p, p);
           }
       }

       // Test a return type matching a parameter.

       //:: error: (contracts.precondition.not.satisfied.field)
       o = methodToCall2(o);
       //:: error: (contracts.precondition.not.satisfied.field) :: error: (assignment.type.incompatible)
       p = methodToCall2(o);
       //:: error: (contracts.precondition.not.satisfied.field)
       methodToCall2(o);
       //:: error: (contracts.precondition.not.satisfied.field)
       methodToCall2(p);
       synchronized(lock2) {
           //:: error: (contracts.precondition.not.satisfied.field)
           o = methodToCall2(o);
           //:: error: (contracts.precondition.not.satisfied.field) :: error: (assignment.type.incompatible)
           p = methodToCall2(o);
           //:: error: (contracts.precondition.not.satisfied.field)
           methodToCall2(o);
           methodToCall2(p);
       }
       synchronized(lock1) {
           o = methodToCall2(o);
           //:: error: (assignment.type.incompatible)
           p = methodToCall2(o);
           methodToCall2(o);
           //:: error: (contracts.precondition.not.satisfied.field)
           methodToCall2(p);
       }

       // Test the receiver type matching a parameter

       // Two @GS parameters with no index are incomparable (as is the case for 'this' and 'q')
       //:: error: (guardsatisfied.parameters.must.match)
       methodToCall3(q);


       //:: error: (guardsatisfied.parameters.must.match) :: error: (contracts.precondition.not.satisfied.field)
       methodToCall3(p);
       synchronized(lock1) {
           // Two @GS parameters with no index are incomparable (as is the case for 'this' and 'q')
           //:: error: (guardsatisfied.parameters.must.match)
           methodToCall3(q);
           //:: error: (guardsatisfied.parameters.must.match) :: error: (contracts.precondition.not.satisfied.field)
           methodToCall3(p);
           synchronized(lock2) {
               // Two @GS parameters with no index are incomparable (as is the case for 'this' and 'q')
               //:: error: (guardsatisfied.parameters.must.match)
               methodToCall3(q);
               //:: error: (guardsatisfied.parameters.must.match)
               methodToCall3(p);
           }
       }

       // Test the return type matching the receiver type

       methodToCall4();
   }

   // Test the return type NOT matching the receiver type
   void testMethodCall(@GuardedBy("lock1") GuardSatisfiedTest this) {
       @GuardedBy("lock2") Object g;
       //:: error: (contracts.precondition.not.satisfied)
       methodToCall4();
       // TODO: contracts.precondition.not.satisfied is getting swallowed below
       //  error (assignment.type.incompatible) error (contracts.precondition.not.satisfied)
       // g = methodToCall4();

       // Separate the above test case into two for now
       //:: error: (contracts.precondition.not.satisfied)
       methodToCall4();

       synchronized(lock1) {
           //:: error: (assignment.type.incompatible)
           g = methodToCall4();
       }
   }

   //:: error: (guardsatisfied.return.must.have.index)
   @GuardSatisfied Object testReturnTypesMustMatchAndMustHaveAnIndex1(@GuardSatisfied Object o) {
       // If the two @GuardSatisfied had an index, this error would not be issued:
       //:: error: (guardsatisfied.assignment.disallowed)
       return o;
   }

   @GuardSatisfied(1) Object testReturnTypesMustMatchAndMustHaveAnIndex2(@GuardSatisfied(1) Object o) {
       return o;
   }

   @GuardSatisfied(0) Object testReturnTypesMustMatchAndMustHaveAnIndex3(@GuardSatisfied(0) Object o) {
       return o;
   }

   // @GuardSatisfied is equivalent to @GuardSatisfied(-1).
   //:: error: (guardsatisfied.return.must.have.index)
   @GuardSatisfied Object testReturnTypesMustMatchAndMustHaveAnIndex4(@GuardSatisfied(-1) Object o) {
       // If the two @GuardSatisfied had an index, this error would not be issued:
       //:: error: (guardsatisfied.assignment.disallowed)
       return o;
   }

   @GuardSatisfied(1) Object testReturnTypesMustMatchAndMustHaveAnIndex5(@GuardSatisfied(2) Object o) {
       //:: error: (return.type.incompatible)
       return o;
   }

   //:: error: (guardsatisfied.return.must.have.index)
   @GuardSatisfied Object testReturnTypesMustMatchAndMustHaveAnIndex6(@GuardSatisfied(2) Object o) {
       //:: error: (return.type.incompatible)
       return o;
   }

   void testParamsMustMatch(@GuardSatisfied(1) Object o, @GuardSatisfied(2) Object p) {
       //:: error: (assignment.type.incompatible)
       o = p;
   }

   void methodToCall1(@GuardSatisfied GuardSatisfiedTest this, @GuardSatisfied(1) Object o, @GuardSatisfied(1) Object p) {
   }

   @GuardSatisfied(1) Object methodToCall2(@GuardSatisfied GuardSatisfiedTest this, @GuardSatisfied(1) Object o) {
       return o;
   }

   void methodToCall3(@GuardSatisfied(1) GuardSatisfiedTest this, @GuardSatisfied(1) Object o) {
   }

   @GuardSatisfied(1) Object methodToCall4(@GuardSatisfied(1) GuardSatisfiedTest this) {
       return this;
   }

   Object lock1, lock2;

   void testAssignment(@GuardSatisfied Object o) {
       @GuardedBy({"lock1", "lock2"}) Object p = new Object();;
       //:: error: (contracts.precondition.not.satisfied.field)
       o = p;
       synchronized(lock1) {
           //:: error: (contracts.precondition.not.satisfied.field)
           o = p;
           synchronized(lock2) {
               o = p;
           }
       }
   }
}

class Foo
{
  @MayReleaseLocks
  void m1() {
  }

  @MayReleaseLocks
  //:: error: (guardsatisfied.with.mayreleaselocks)
  void m2(@GuardSatisfied Foo f) {
      //:: error: (method.invocation.invalid)
      f.m1();
  }

  @MayReleaseLocks
  void m2_2(Foo f) {
    f.m1();
  }

  void m3(@GuardSatisfied Foo f) {
      //:: error: (method.guarantee.violated)
      f.m1(); // TODO: Fix: This should error with method.invocation.invalid but it gets swallowed and only method.guarantee.violated is output.
  }

  @MayReleaseLocks
  void m4(Foo f) {
      f.m1();
  }

  @MayReleaseLocks
  void m5(Foo f) {
      m3(f);
  }

  /*@MayReleaseLocks
  void m6(@GuardSatisfied Foo f) {
      m2_2(f);
  }*/
}
