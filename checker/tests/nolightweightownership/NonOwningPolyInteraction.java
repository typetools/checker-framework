// A test that non-owning method parameters are really treated as @MustCall({})
// wrt polymorphic types. Based on some false positives in Zookeeper.
//
// This version is modified to expect that owning/notowning annotations do nothing,
// for the -AnoLightweightOwnership flag.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;

class NonOwningPolyInteraction {
  void foo(@NotOwning InputStream instream) {
    @MustCall({})
    BufferedInputStream bis = new BufferedInputStream(instream);
    @MustCall({"close"})
    BufferedInputStream bis2 = new BufferedInputStream(instream);
  }

  void bar(@Owning InputStream instream) {
    @MustCall({})
    BufferedInputStream bis = new BufferedInputStream(instream);
    @MustCall({"close"})
    BufferedInputStream bis2 = new BufferedInputStream(instream);
  }

  // default anno for params in @NotOwning
  void baz(InputStream instream) {
    @MustCall({})
    BufferedInputStream bis = new BufferedInputStream(instream);
    @MustCall({"close"})
    BufferedInputStream bis2 = new BufferedInputStream(instream);
  }

  NonOwningPolyInteraction(@NotOwning InputStream instream) {
    @MustCall({})
    BufferedInputStream bis = new BufferedInputStream(instream);
    @MustCall({"close"})
    BufferedInputStream bis2 = new BufferedInputStream(instream);
  }

  // extra param(s) here and on the next constructor because Java requires constructors to have
  // different signatures.
  NonOwningPolyInteraction(@Owning InputStream instream, int x) {
    @MustCall({})
    BufferedInputStream bis = new BufferedInputStream(instream);
    @MustCall({"close"})
    BufferedInputStream bis2 = new BufferedInputStream(instream);
  }

  // default anno for params in @NotOwning
  NonOwningPolyInteraction(InputStream instream, int x, int y) {
    @MustCall({})
    BufferedInputStream bis = new BufferedInputStream(instream);
    @MustCall({"close"})
    BufferedInputStream bis2 = new BufferedInputStream(instream);
  }
}
