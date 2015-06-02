// @skip-tests  type.invalid is currently hard to support in the qualifier api

// Test case for Issue 292:
// https://code.google.com/p/checker-framework/issues/detail?id=292

// TODO: ensure that type validation is consistently performed for each
// possible tree.
// We should also add a jtreg version of this test to
// ensure that each error is only output once and in the right place.

import org.checkerframework.checker.regex.qual.*;
import org.checkerframework.qualframework.poly.qual.Wildcard;

abstract class TypeInvalid {
    // Duplication forbidden
    //:: error: (type.invalid)
    void bad(@Regex @Regex(1) TypeInvalid c) {
        //:: error: (type.invalid)
        Object o = new @Regex @Regex(1) Object();
        //:: error: (type.invalid)
        o = new @Regex @Regex(1) Object();
        //:: error: (type.invalid)
        o = o.equals(new @Regex @Regex(1) Object());
        //:: error: (type.invalid)
        o = (Object) new @Regex @Regex(1) Object();
        //:: error: (type.invalid)
        o = (@Regex @Regex(1) TypeInvalid) o;
        //:: error: (type.invalid)
        o = (new @Regex @Regex(1) Object()) instanceof Object;
        //:: error: (type.invalid)
        o = o instanceof @Regex @Regex(1) TypeInvalid;
    }

    //:: error: (type.invalid)
    @Regex @Regex(1) Object bar() {
        return null;
    }

    //:: error: (type.invalid)
    abstract @Regex @Regex(1) Object absbar();

    void voidmethod() {}

    TypeInvalid() {}

    //:: error: (type.invalid)
    @Regex @Regex(1) TypeInvalid(int p) {}

    //:: error: (type.invalid)
    void recv(@Regex @Regex(1) TypeInvalid this) {}

    //:: error: (type.invalid)
    @Regex @Regex(1) Object field;

    // TODO: Note the error marker positions for the errors on fields
    // and method return types. Maybe these should be improved.

    //:: error: (type.invalid)
    void athro() throws @Regex @Regex(1) Exception { }
}
