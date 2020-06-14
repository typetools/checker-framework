// Test case for https://github.com/kelloggm/checker-framework/issues/156

// @skip-test until the issue is fixed

import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.MinLenFieldInvariant;

public class MinLenFieldInvar2 {
    class Super {
        public final int @MinLen(2) [] minlen2;

        public Super(int @MinLen(2) [] minlen2) {
            this.minlen2 = minlen2;
        }
    }

    @MinLenFieldInvariant(field = "minlen2", minLen = 4)
    class ValidSub extends Super {
        public ValidSub(int[] validSubField) {
            super(new int[] {1, 2, 3, 4});
        }
    }

    void useAtValidSub(Super s) {
        if (s instanceof ValidSub) {
            System.out.println(s.minlen2[3]);
            ValidSub vs = (ValidSub) s;
            System.out.println(vs.minlen2[3]);
        }
    }
}
