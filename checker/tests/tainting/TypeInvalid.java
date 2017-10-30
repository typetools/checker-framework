// Test case for Issue 292:
// https://github.com/typetools/checker-framework/issues/292

// TODO: ensure that type validation is consistently performed for each
// possible tree.
// We should also add a jtreg version of this test to
// ensure that each error is only output once and in the right place.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

abstract class TypeInvalid {
    // Duplication forbidden
    // :: error: (type.invalid)
    void bad(@Tainted @Untainted TypeInvalid c) {
        // :: error: (type.invalid)
        Object o = new @Tainted @Untainted Object();
        // :: error: (type.invalid)
        o = new @Tainted @Untainted Object();
        // :: error: (type.invalid)
        o = o.equals(new @Tainted @Untainted Object());
        // :: error: (type.invalid)
        o = (Object) new @Tainted @Untainted Object();
        // :: error: (type.invalid)
        o = (@Tainted @Untainted TypeInvalid) o;
        // :: error: (type.invalid)
        o = (new @Tainted @Untainted Object()) instanceof Object;
        // :: error: (type.invalid)
        o = o instanceof @Tainted @Untainted TypeInvalid;
    }

    // :: error: (type.invalid)
    @Tainted @Untainted Object bar() {
        return null;
    }

    // :: error: (type.invalid)
    abstract @Tainted @Untainted Object absbar();

    void voidmethod() {}

    TypeInvalid() {}

    // :: error: (type.invalid)
    @Tainted @Untainted TypeInvalid(int p) {}

    // :: error: (type.invalid)
    void recv(@Tainted @Untainted TypeInvalid this) {}

    // :: error: (type.invalid)
    @Tainted @Untainted Object field;

    // TODO: Note the error marker positions for the errors on fields
    // and method return types. Maybe these should be improved.

    // :: error: (type.invalid)
    void athro() throws @Tainted @Untainted Exception {}
}
