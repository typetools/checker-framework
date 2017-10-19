import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.interning.qual.*;

public class PrimitivesInterning {

    void test() {
        int a = 3;

        if (a == 3) {
            System.out.println("yes");
        } else {
            System.out.println("no");
        }

        if (a != 2) {
            System.out.println("yes");
        } else {
            System.out.println("no");
        }

        String name = "Interning";
        if ((name.indexOf('[') == -1) && (name.indexOf('(') == -1)) {
            System.out.println("has no open punctuation");
        } else {
            System.out.println("has open punctuation");
        }

        Number n = new Integer(22);
        boolean is_double = (n instanceof Double); // valid

        int index = 0;
        index = Integer.decode("22"); // valid: auto-unboxing conversion

        // auto-unboxing conversion again
        Map<String, Integer> m = new HashMap<String, Integer>();
        if (m.get("hello") == 22) {
            System.out.println("hello maps to 22");
        }
    }

    public static int pow_fast(int base, int expt) throws ArithmeticException {
        if (expt < 0) {
            throw new ArithmeticException("Negative base passed to pow");
        }

        int this_square_pow = base;
        int result = 1;
        while (expt > 0) {
            if ((expt & 1) != 0) {
                result *= this_square_pow;
            }
            expt >>= 1;
            this_square_pow *= this_square_pow;
        }
        return result;
    }

    /** Return the greatest common divisor of the two arguments. */
    public static int gcd(int a, int b) {

        // Euclid's method
        if (b == 0) {
            return (Math.abs(a));
        }
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int tmp = b;
            b = a % b;
            a = tmp;
        }
        return a;
    }

    /** Return the greatest common divisor of the elements of int array a. */
    public static int gcd(int[] a) {
        // Euclid's method
        if (a.length == 0) {
            return 0;
        }
        int result = a[0];
        for (int i = 1; i < a.length; i++) {
            result = gcd(a[i], result);
            if ((result == 1) || (result == 0)) {
                return result;
            }
        }
        return result;
    }

    /**
     * Return the gcd (greatest common divisor) of the differences between the elements of int array
     * a.
     */
    public static int gcd_differences(int[] a) {
        // Euclid's method
        if (a.length < 2) {
            return 0;
        }
        int result = a[1] - a[0];
        for (int i = 2; i < a.length; i++) {
            result = gcd(a[i] - a[i - 1], result);
            if ((result == 1) || (result == 0)) {
                return result;
            }
        }
        return result;
    }

    void compounds() {
        int res = 0;
        res += 5;
        res /= 9;
    }

    // TODO: enable after boxing is improved in AST creation
    // void negation() {
    //   Boolean t = new Boolean(true);
    //   boolean b = !t;
    // }

}
