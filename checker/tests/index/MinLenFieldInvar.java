package fieldinvar;

import org.checkerframework.checker.index.qual.*;
import org.checkerframework.checker.index.qual.MinLenFieldInvariant;
import org.checkerframework.framework.qual.FieldInvariant;

public class MinLenFieldInvar {
    class Super {
        public final int @MinLen(2) [] minlen2;

        public Super(int @MinLen(2) [] minlen2) {
            this.minlen2 = minlen2;
        }
    }

    //:: error: (field.invar.not.subtype)
    @MinLenFieldInvariant(field = "minlen2", minLen = 1)
    class InvalidSub extends Super {
        public InvalidSub() {
            super(new int[] {1, 2});
        }
    }

    @MinLenFieldInvariant(field = "minlen2", minLen = 4)
    class ValidSub extends Super {
        public final int[] validSubField;

        public ValidSub(int[] validSubField) {
            super(new int[] {1, 2, 3, 4});
            this.validSubField = validSubField;
        }
    }

    //:: error: (field.invar.not.found.superclass)
    @MinLenFieldInvariant(field = "validSubField", minLen = 3)
    class InvalidSubSub1 extends ValidSub {
        public InvalidSubSub1() {
            super(new int[] {1, 2});
        }
    }

    //:: error: (field.invar.not.subtype.superclass)
    @MinLenFieldInvariant(field = "minlen2", minLen = 3)
    class InvalidSubSub2 extends ValidSub {
        public InvalidSubSub2() {
            super(new int[] {1, 2});
        }
    }

    @FieldInvariant(field = "minlen2", qualifier = MinLenBottom.class)
    @MinLenFieldInvariant(field = "validSubField", minLen = 4)
    class ValidSubSub extends ValidSub {
        public ValidSubSub() {
            super(null);
        }

        void test() {
            int @MinLenBottom [] bot = minlen2;
            int @MinLen(4) [] four = validSubField;
        }
    }
}
