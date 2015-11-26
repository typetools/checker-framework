import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.GuardedBy;

public class GuardSatisfiedTest {
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

       methodToCall3(q);
       //:: error: (guardsatisfied.parameters.must.match) :: error: (contracts.precondition.not.satisfied.field)
       methodToCall3(p);
       synchronized(lock1) {
           methodToCall3(q);
           //:: error: (guardsatisfied.parameters.must.match) :: error: (contracts.precondition.not.satisfied.field)
           methodToCall3(p);
           synchronized(lock2) {
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
   @GuardSatisfied Object testReturnTypesMustMatch1(@GuardSatisfied Object o) {
       return o;
   }

   @GuardSatisfied(1) Object testReturnTypesMustMatch2(@GuardSatisfied(1) Object o) {
       return o;
   }

   @GuardSatisfied(0) Object testReturnTypesMustMatch3(@GuardSatisfied(0) Object o) {
       return o;
   }

   // @GuardSatisfied is equivalent to @GuardSatisfied(-1).
   //:: error: (guardsatisfied.return.must.have.index)
   @GuardSatisfied Object testReturnTypesMustMatch4(@GuardSatisfied(-1) Object o) {
       return o;
   }

   @GuardSatisfied(1) Object testReturnTypesMustMatch5(@GuardSatisfied(2) Object o) {
       //:: error: (return.type.incompatible)
       return o;
   }

   //:: error: (guardsatisfied.return.must.have.index)
   @GuardSatisfied Object testReturnTypesMustMatch6(@GuardSatisfied(2) Object o) {
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
