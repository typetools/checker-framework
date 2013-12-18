// Test case for issue 292: https://code.google.com/p/checker-framework/issues/detail?id=292

import checkers.tainting.quals.*;

class TypeInvalid {
    // Duplication forbidden
    //:: error: (type.invalid)
    void bad(@Tainted @Untainted TypeInvalid c) {
        //:: error: (type.invalid)
        Object o = new @Tainted @Untainted TypeInvalid();
        //:: error: (type.invalid)
        o = new @Tainted @Untainted TypeInvalid();
        //:: error: (type.invalid)
        o = o.equals(new @Tainted @Untainted TypeInvalid());
        //:: error: (type.invalid)
        o = (Object) new @Tainted @Untainted TypeInvalid();
        //:: error: (type.invalid)
        o = (@Tainted @Untainted TypeInvalid) o;
        //:: error: (type.invalid)
        o = (new @Tainted @Untainted TypeInvalid()) instanceof Object;
        //:: error: (type.invalid)
        o = o instanceof @Tainted @Untainted TypeInvalid;

        // TODO: ensure that type validation is consistently performed for each
        // possible tree.
        // We should also add a jtreg version of this test to
        // ensure that each error is only output once.
    }

    //:: error: (type.invalid)
    @Tainted @Untainted Object bar() { return null; }

    //:: error: (type.invalid)
    @Tainted @Untainted Object field;

    // TODO: Note the error marker positions for the errors on fields
    // and method return types. Maybe these should be improved.

    //:: error: (type.invalid)
    void athro() throws @Tainted @Untainted Exception { }
}
