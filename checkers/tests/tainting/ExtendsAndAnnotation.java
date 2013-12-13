// Test case for issue 278: https://code.google.com/p/checker-framework/issues/detail?id=278

import checkers.tainting.quals.*;

class ExtendsAndAnnotation extends @Tainted Object {
    void test(@Untainted ExtendsAndAnnotation c) {
        Object o = new @Untainted ExtendsAndAnnotation();
        o = new @Tainted ExtendsAndAnnotation();
    }

    // Duplication forbidden
    //:: error: (type.invalid)
    void bad(@Tainted @Untainted ExtendsAndAnnotation c) {
        //:: error: (type.invalid)
        Object o = new @Tainted @Untainted ExtendsAndAnnotation();
        //:: error: (type.invalid)
        o = new @Tainted @Untainted ExtendsAndAnnotation();
        //:: error: (type.invalid)
        o = o.equals(new @Tainted @Untainted ExtendsAndAnnotation());
        //:: error: (type.invalid)
	o = (Object) new @Tainted @Untainted ExtendsAndAnnotation();
        //:: error: (type.invalid)
        o = (@Tainted @Untainted ExtendsAndAnnotation) o;
        //:: error: (type.invalid)
        o = (new @Tainted @Untainted ExtendsAndAnnotation()) instanceof Object;
        //:: error: (type.invalid)
        o = o instanceof @Tainted @Untainted ExtendsAndAnnotation;

        // TODO: for at least one type system we should ensure
        // that type validation is consistently performed for each
        // possible tree.
        // We should also add a jtreg version of this test to
        // ensure that each error is only output once.
    }
}
