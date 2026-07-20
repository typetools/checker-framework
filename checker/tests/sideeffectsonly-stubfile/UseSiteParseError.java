// The `@SideEffectsOnly` annotations on `Library` come from `seonly.astub`.  One of them names a
// field that does not exist.  Besides the error at the declaration, the checker must also fail
// closed at each call site, rather than treat the callee as side-effecting less than it was
// declared to.  When `Library` is read from bytecode rather than compiled alongside this file,
// the call-site error is the only one reported.

package sideeffectsonly.stubfile;

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class UseSiteParseError {

  @SideEffectsOnly("#1")
  void callUnparseable(Library lib) {
    // :: error: (purity.unknown.sideeffectsonly)
    lib.unparseable();
  }

  @SideEffectsOnly("#1")
  void callParseable(Library lib) {
    lib.parseable();
  }
}
