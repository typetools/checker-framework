// Test case for Issue 292:
// https://github.com/typetools/checker-framework/issues/292

// TODO: ensure that type validation is consistently performed for each possible tree.
// We should also add a jtreg version of this test to
// ensure that each error is only output once and in the right place.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

abstract class TypeInvalid {
  // :: error: (conflicting.annos)
  static @Untainted @Tainted class Inner {}
  // Duplication forbidden
  // :: error: (conflicting.annos)
  void bad(@Tainted @Untainted TypeInvalid c) {
    // :: error: (conflicting.annos)
    Object o = new @Tainted @Untainted Object();
    // :: error: (conflicting.annos)
    o = new @Tainted @Untainted Object();
    // :: error: (conflicting.annos)
    o = o.equals(new @Tainted @Untainted Object());
    // :: error: (conflicting.annos)
    o = (Object) new @Tainted @Untainted Object();
    // :: error: (conflicting.annos)
    o = (@Tainted @Untainted TypeInvalid) o;
    // :: error: (conflicting.annos)
    o = (new @Tainted @Untainted Object()) instanceof Object;
    // :: error: (conflicting.annos)
    // :: warning: (instanceof.unsafe)
    o = o instanceof @Tainted @Untainted TypeInvalid;
  }

  // :: error: (conflicting.annos)
  @Tainted @Untainted Object bar() {
    return null;
  }

  // :: error: (conflicting.annos)
  abstract @Tainted @Untainted Object absbar();

  void voidmethod() {}

  TypeInvalid() {}

  // :: error: (conflicting.annos)
  @Tainted @Untainted TypeInvalid(int p) {}

  // :: error: (conflicting.annos)
  void recv(@Tainted @Untainted TypeInvalid this) {}

  // :: error: (conflicting.annos)
  @Tainted @Untainted Object field;

  // TODO: Note the error marker positions for the errors on fields
  // and method return types. Maybe these should be improved.

  // :: error: (conflicting.annos)
  void athro() throws @Tainted @Untainted Exception {}
}
