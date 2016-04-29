import org.checkerframework.checker.lock.qual.GuardedBy;

public class ViewpointAdaptation {

    class MyClass { Object field; }

    class LockExample {
        protected final Object myLock = new Object();

        @GuardedBy("myLock")
        protected MyClass locked;

        @GuardedBy("this.myLock")
        protected MyClass locked2;

        public void accessLock() {
            synchronized(myLock) {
                this.locked.field = new Object();
            }
        }
    }

    class LockExampleSubclass extends LockExample {
        private final Object myLock = new Object();

        @GuardedBy("myLock")
        private MyClass locked;

        public LockExampleSubclass() {
            final LockExampleSubclass les1 = new LockExampleSubclass();
            final LockExampleSubclass les2 = new LockExampleSubclass();
            final LockExampleSubclass les3 = les2;
            LockExample le1 = new LockExample();

            synchronized(super.myLock){
                super.locked.toString();
                super.locked2.toString();
                //:: error: (contracts.precondition.not.satisfied)
                locked.toString();
            }
            synchronized(myLock){
                //:: error: (contracts.precondition.not.satisfied)
                super.locked.toString();
                //:: error: (contracts.precondition.not.satisfied)
                super.locked2.toString();
                locked.toString();
            }

            //:: error: (assignment.type.incompatible)
            les1.locked = le1.locked;
            //:: error: (assignment.type.incompatible)
            les1.locked = le1.locked2;

            //:: error: (assignment.type.incompatible)
            les1.locked = les2.locked;


            //:: error: (assignment.type.incompatible)
            this.locked = super.locked;
            //:: error: (assignment.type.incompatible)
            this.locked = super.locked2;
        }

        @Override
        public void accessLock() {
            synchronized(myLock) {
                this.locked.field = new Object();
                //:: error: (contracts.precondition.not.satisfied.field)
                super.locked.field = new Object();
                System.out.println(this.locked.field + " " +
                        //:: error: (contracts.precondition.not.satisfied.field)
                    super.locked.field);
                System.out.println("Are locks equal? " +
                        (super.locked == this.locked ? "yes" : "no"));
            }

        }
    }

    class Class1 {
        public final Object lock = new Object();
        @GuardedBy("lock") MyClass m = new MyClass();
    }

    class Class2 {
        public final Object lock = new Object();
        @GuardedBy("lock") MyClass m = new MyClass();

        void method(final Class1 a) {
            final Object lock = new Object();
            @GuardedBy("lock") MyClass local = new MyClass();
            //:: error: (contracts.precondition.not.satisfied.field)
            local.field = new Object();


            synchronized(lock) {
                //:: error: (contracts.precondition.not.satisfied.field)
                a.m.field = new Object();
            }
            synchronized(this.lock) {
                //:: error: (contracts.precondition.not.satisfied.field)
                a.m.field = new Object();
            }
            synchronized(a.lock) {
                a.m.field = new Object();
            }

            synchronized(lock) {
                local.field = new Object();
            }
            synchronized(this.lock) {
                //:: error: (contracts.precondition.not.satisfied.field)
                local.field = new Object();
            }
            synchronized(a.lock) {
                //:: error: (contracts.precondition.not.satisfied.field)
                local.field = new Object();
            }

            synchronized(lock) {
                //:: error: (contracts.precondition.not.satisfied.field)
                this.m.field = new Object();
                //:: error: (contracts.precondition.not.satisfied.field)
                m.field = new Object();
            }
            synchronized(this.lock) {
                this.m.field = new Object();
                m.field = new Object();
            }
            synchronized(a.lock) {
                //:: error: (contracts.precondition.not.satisfied.field)
                this.m.field = new Object();
                //:: error: (contracts.precondition.not.satisfied.field)
                m.field = new Object();
            }
        }
    }
}

