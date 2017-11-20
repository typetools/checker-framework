import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import testlib.util.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface OddInt {
    @Odd int value();
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface OddIntArr {
    @Odd int[] value();
}

@interface OddRec {
    OddIntArr[] value();
}

class Const {
    @SuppressWarnings("test")
    public static final @Odd int ok1 = 5;

    @SuppressWarnings("test")
    public static final @Odd int ok2 = 5;

    public static final int notodd = 4;
}

class Uses {
    @OddInt(Const.ok1)
    Object good1;

    // :: error: (annotation.type.incompatible)
    @OddInt(4)
    Object bad1;

    // :: error: (annotation.type.incompatible)
    @OddInt(Const.notodd)
    Object bad2;

    @OddIntArr(Const.ok1)
    Object good2;

    @OddIntArr({Const.ok1, Const.ok2})
    Object good3;

    // :: error: (annotation.type.incompatible)
    @OddIntArr(4)
    Object bada1;

    // :: error: (annotation.type.incompatible)
    @OddIntArr({Const.ok1, 4})
    Object bada2;

    @OddRec(@OddIntArr({Const.ok1, Const.ok2}))
    void goodrec1() {}

    @OddRec({@OddIntArr({Const.ok1, Const.ok2}), @OddIntArr({Const.ok1, Const.ok2})})
    void goodrec2() {}

    // :: error: (annotation.type.incompatible)
    @OddRec(@OddIntArr({Const.ok1, 4}))
    void badrec1() {}

    // :: error: (annotation.type.incompatible)
    @OddRec({@OddIntArr({Const.ok1, Const.ok2}), @OddIntArr({3, Const.ok2})})
    void badrec2() {}
}
