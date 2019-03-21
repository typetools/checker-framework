// Example code from the user manual

import org.checkerframework.checker.signedness.qual.Unsigned;

public class ManualExample {

    void m() {

        int s1 = -2;
        int s2 = -1;

        @Unsigned int u1 = 0xFFFFFFFE; // unsigned: 2^32 - 2, signed: -2
        @Unsigned int u2 = 0xFFFFFFFF; // unsigned: 2^32 - 1, signed: -1

        int result;

        result = s1 / s2; // OK: result is 2, which is correct for -2 / -1
        // :: error: (operation.unsignedlhs) :: error: (assignment.type.incompatible)
        result = u1 / u2; // ERROR: result is 2, which is incorrect for (2^32 - 2) / (2^32 - 1)

        int s3 = -1;
        int s4 = 5;

        @Unsigned int u3 = 0xFFFFFFFF; // unsigned: 2^32 - 1, signed: -1
        @Unsigned int u4 = 5;

        result = s3 % s4; // OK: result is -1, which is correct for -1 % 5
        // :: error: (operation.unsignedlhs) :: error: (assignment.type.incompatible)
        result = u3 % u4; // ERROR: result is -1, which is incorrect for (2^32 - 1) % 5
    }
}
