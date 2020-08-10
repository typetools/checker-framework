import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.InternedDistinct;

public class FindDistinctTest {

    public void ok1(@FindDistinct Object o) {
        // TODO: The fact that this type-checks is an (undesired) artifact of the current
        // implementation of @FindDistinct.
        @InternedDistinct Object o2 = o;
    }

    public void ok2(@FindDistinct Object findIt, Object other) {
        boolean b = findIt == other;
    }

    public void useOk1(Object notinterned, @Interned Object interned) {
        ok1(notinterned);
        ok1(interned);
    }

    public void bad1(Object o) {
        // :: error: (assignment.type.incompatible)
        @InternedDistinct Object o2 = o;
    }

    public void bad2(Object findIt, Object other) {
        // :: error: (not.interned)
        boolean b = findIt == other;
    }
}
