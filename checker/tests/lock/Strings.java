import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;

public class Strings {
    final Object lock = new Object();

    // These casts are safe because if the casted Object is a String, it must be @GuardedBy({})
    void StringIsGBnothing(
            @GuardedByUnknown Object o1,
            @GuardedBy("lock") Object o2,
            @GuardSatisfied Object o3,
            @GuardedByBottom Object o4) {
        String s1 = (String) o1;
        String s2 = (String) o2;
        String s3 = (String) o3;
        String s4 = (String) o4; // OK
    }

    // Tests that the resulting type of string concatenation is always @GuardedBy({})
    // (and not @GuardedByUnknown, which is the LUB of @GuardedBy({}) (the type of the
    // string literal "a") and @GuardedBy("lock") (the type of param))
    void StringConcat(@GuardedBy("lock") MyClass param) {
        {
            String s1a = "a" + "a";
            // :: error: (lock.not.held)
            String s1b = "a" + param;
            // :: error: (lock.not.held)
            String s1c = param + "a";
            // :: error: (lock.not.held)
            String s1d = param.toString();

            String s2 = "a";
            // :: error: (lock.not.held)
            s2 += param;

            String s3 = "a";
            // In addition to testing whether "lock" is held, tests that the result of a string
            // concatenation has type @GuardedBy({}).
            // :: error: (lock.not.held)
            String s4 = s3 += param;
        }
        synchronized (lock) {
            String s1a = "a" + "a";
            String s1b = "a" + param;
            String s1c = param + "a";
            String s1d = param.toString();

            String s2 = "a";
            s2 += param;

            String s3 = "a";
            // In addition to testing whether "lock" is held, tests that the result of a string
            // concatenation has type @GuardedBy({}).
            String s4 = s3 += param;
        }
    }

    class MyClass {
        Object field = new Object();
    }
}
