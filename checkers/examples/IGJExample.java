import checkers.igj.quals.*;

/**
 * An Immutable class that represent a date
 */
@Immutable
class Date {
    int time; // epoch time

    public Date(int time) @AssignsFields {
        this.time = time;
    }

    public int getYear() { return 0; }
    public int getMonth() { return 0; }
    public int getDay() { return 0; }

    public int testMutate() {
        this.time = 4;  // Error: Cannot re-assign in a method with RO receiver
        this.time++;    // Error: Cannot re-assign in a method with RO receiver
    }

    public int mutableReciever() /*@Mutable*/ // Error: No method with mutable receiver within Immutable Class
    { }
}

@I
class Point {
    double x;
    double y;

    Point(double x, double y) @AssignsFields {
        setX(x);
        setY(y);
    }

    void setX(double x) @AssignsFields { this.x = x; }
    void setY(double y) @AssignsFields { this.y = y; }

    double getX() /*@ReadOnly*/ { return x; }
    double getY() /*@ReadOnly*/ { return y; }

    public int hashCode() /*@ReadOnly*/ {
        x += 4; // Error: ReadOnly receiver
    }

    public static @I Point getMidPoint(@I Point p1, @I Point p2) {
        return new @I Point((p1.getX() + p2.getX()) / 2.0,
                            (p1.getY() + p2.getY()) / 2.0);
    }

    public static void test() {
        @Immutable Point p1 = new /*@Immutable*/ Point(1,1);
        @Immutable Point p2 = new /*@Mutable*/ Point (1, 1); // Error: Incompatible types
        @Mutable Point p3 = new /*@Mutable*/ Point(1,1);

        @ReadOnly Point p4 = new /*@Mutable*/ Point(1, 1);

        p1.setX(4); // Error: Cannot mutate an immutable object
        p4.setX(4); // Error: Cannot mutate through a readOnly reference

        @Immutable Point mp1 = getMidPoint(p1, p1); // return value resolves to Immutable
        @Immutable Point mp2 = getMidPoint(p3, p3); // Error: return value resolves to Mutable

        @Mutable Point mp3 = getMidPoint(p3, p3); // resolves to Mutable

        @Immutable Point mp4 = getMidPoint(p4, p4); // Error: return value resolves to Immutable
    }
}

@I
class TestClass {

    void mutableReceiver() /*@Mutable*/ { }
    void readOnlyReceiver() /*@ReadOnly*/ { }
    void immutableReceiver() /*@Immutable*/ { }

    static void isMutable(@Mutable TestClass tc)  { }
    static void isImmutable(@Immutable TestClass tc) { }
    static void isRO(@ReadOnly TestClass tc) { }

    TestClass() @AssignsFields {
        readOnlyReceiver();
        mutableReceiver();   // Error: cannot call method with mutable receiver
        immutableReceiver();  // Error: Cannot call method with immutable receiver
    }

    void testMethod1() /*@ReadOnly*/ {
        readOnlyReceiver();
        mutableReceiver();   // Error: cannot call method with mutable receiver within method with ReadOnly receiver
        isRO(this);
        isMutable(this);    // Error: this escapes as RO
    }

    void testMethod2() /*@Mutable*/ {
        immutableReceiver();  // Error: cannot call method with immutable receiver
        isRO(this);
        isMutable(this);    // this escapes as mutable
        isImmutable(this);  // Error: this escapes as Immutable
    }

    void testMethod3() /*@Immutable*/ {
        immutableReceiver();
        isRO(this);
        isMutable(this);    // Error: this escapes as immutable
        isImmutable(this);  // this escapes as mutable
    }

}
