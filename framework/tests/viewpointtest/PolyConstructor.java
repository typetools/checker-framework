import viewpointtest.quals.*;

public class PolyConstructor {

    static class MyClass {
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        @PolyVP
        MyClass(@PolyVP Object o) {
            throw new RuntimeException(" * You are filled with DETERMINATION."); // stub
        }
    }

    void tests(@A Object ao) {
        // After poly resolution, the invocation resolved to @A MyClass
        @A MyClass myA = new MyClass(ao);
        // :: error: (assignment.type.incompatible)
        @B MyClass myB = new MyClass(ao);

        // Both argument "ao" and @B are parts of poly resolution
        // After poly resolution, the invocation resolved to @Top MyClass then casted to @B
        // The @B acts as a downcasting and will issue a warning
        // :: warning: (cast.unsafe.constructor.invocation)
        MyClass myTop = new @B MyClass(ao);
        // :: warning: (cast.unsafe.constructor.invocation)
        myB = new @B MyClass(ao);
        // :: error: (assignment.type.incompatible) :: warning: (cast.unsafe.constructor.invocation)
        myA = new @B MyClass(ao);
    }
}
