import java.util.*;
import checkers.igj.quals.*;

@I
class ForEnhanced {

    @Mutable List<@Mutable ForEnhanced> mm = new @Mutable LinkedList<@Mutable ForEnhanced>();
    @Mutable List<@ReadOnly ForEnhanced> mr = new @Mutable LinkedList<@ReadOnly ForEnhanced>();
    @ReadOnly List<@Mutable ForEnhanced> rm = mm;
    @ReadOnly List<@ReadOnly ForEnhanced> rr = mr;

    @Mutable ForEnhanced @Mutable [] mma;
    @ReadOnly ForEnhanced @ReadOnly [] mra;
    @Mutable ForEnhanced @ReadOnly [] rma;
    @ReadOnly ForEnhanced @ReadOnly [] rra;

    @ReadOnly List<@ReadOnly List<@Mutable ForEnhanced>> rrm;

    void testMutable() {
        for (@Mutable ForEnhanced o : mm);
        for (@Mutable ForEnhanced o : mr);  // should emit error
        for (@Mutable ForEnhanced o : rm);
        for (@Mutable ForEnhanced o : rr);  // should emit error
        for (@ReadOnly ForEnhanced o : mm);
        for (@ReadOnly ForEnhanced o : mr);
        for (@ReadOnly ForEnhanced o : rm);
        for (@ReadOnly ForEnhanced o : rr);

        for (@Mutable ForEnhanced o : mma);
        for (@Mutable ForEnhanced o : mra);  // should emit error
        for (@Mutable ForEnhanced o : rma);
        for (@Mutable ForEnhanced o : rra);  // should emit error
        for (@ReadOnly ForEnhanced o : mma);
        for (@ReadOnly ForEnhanced o : mra);
        for (@ReadOnly ForEnhanced o : rma);
        for (@ReadOnly ForEnhanced o : rra);

        for (@Mutable List<@ReadOnly ForEnhanced> w : rrm);   // should emit error

        for (@ReadOnly List<@ReadOnly ForEnhanced> rr2 : rrm) {
            for (@Mutable ForEnhanced no : rr2);     // should emit error
            for (@ReadOnly ForEnhanced yes : rr2);
        }

        for (@ReadOnly List<@Mutable ForEnhanced> rm2 : rrm) {
            for (@Mutable ForEnhanced yes : rm2);
            for (@ReadOnly ForEnhanced yes : rm2);
        }

    }

    public @Mutable List<@ReadOnly ForEnhanced> getMR() { return mr; }
    public @ReadOnly ForEnhanced @ReadOnly [] getRRA() { return rra; }
    public @Mutable ForEnhanced @Mutable [] getMMA() { return mma; }

    void testMethods() {
        for (@Mutable ForEnhanced o : getMR()); // should emit error
        for (@ReadOnly ForEnhanced o : getMR());

        for (@Mutable ForEnhanced o : getRRA()); // should emit error
        for (@ReadOnly ForEnhanced o : getRRA());

        for (@Mutable ForEnhanced o : getMMA());
    }

    @Mutable class ForEnhancedList extends @Mutable LinkedList<@ReadOnly ForEnhanced> { };
    @Mutable class ForEnhancedIterable implements @Mutable Iterable<@ReadOnly ForEnhanced> {
        public Iterator<@ReadOnly ForEnhanced> iterator(@ReadOnly ForEnhancedIterable this) { return null; }
    }

    // Test more iterables
    void testIterables() {
        ForEnhancedList l1 = new ForEnhancedList();
        for (@Mutable ForEnhanced s : l1);    // should emit error

        ForEnhancedIterable l2 = new ForEnhancedIterable();
        for (@Mutable ForEnhanced s : l2);    // should emit error
    }

    // Test Expressions without Elements

    void testConditionalExpress() {
        for (@Mutable ForEnhanced o : true ? getRRA() : getMMA()); // should emit error
        for (@ReadOnly ForEnhanced o : true ? getRRA() : getMMA());
    }


    void testNewForEnhanceds() {
        for (@Mutable ForEnhanced str : new @Mutable ArrayList<@ReadOnly ForEnhanced>());   // should emit error
        for (@Mutable ForEnhanced str : new @Mutable ForEnhancedList());    // should emit error
        for (@Mutable ForEnhanced str : new @Mutable ForEnhancedList());    // should emit error
    }

    void test() {
        for (;;) { }
    }
}
