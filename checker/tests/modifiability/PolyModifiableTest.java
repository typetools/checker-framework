import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeModifiable;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.PolyIteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.PolyModifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

/**
 * Tests @PolyModifiable, which expands
 * to @PolyGrowable @PolySeqGrowable @PolyShrinkable @PolyReplaceable and thus preserves all four
 * capabilities independently. The alias does not expand into the Iterator hierarchy, so a method
 * that must also preserve @IteratorPolyMod writes @PolyIteratorPolyMod explicitly, as `identity`
 * below does.
 */
public class PolyModifiableTest {

  /** A simple polymorphic identity method that preserves all five hierarchies. */
  @PolyModifiable @PolyIteratorPolyMod List<String> identity(@PolyModifiable @PolyIteratorPolyMod List<String> x) {
    return x;
  }

  /** A Deque-specific identity method for testing SeqGrow alias expansion on all supported JDKs. */
  @PolyModifiable Deque<String> identityDeque(@PolyModifiable Deque<String> x) {
    return x;
  }

  /** A Map.Entry-specific identity method; only the replace capability is carried. */
  Map.@PolyModifiable Entry<String, String> identityEntry(
      Map.@PolyModifiable Entry<String, String> x) {
    return x;
  }

  void testPoly(
      @Modifiable List<String> mod,
      @Modifiable @IteratorPolyMod List<String> modIteratorPoly,
      @Growable @Shrinkable List<String> gs, // Grow+Shrink; Replace = MaybeReplaceable (default)
      @Growable @Replaceable List<String> gr, // Grow+Replace; Shrink = MaybeShrinkable
      @Shrinkable @Replaceable List<String> sr, // Shrink+Replace; Grow = MaybeGrowable
      @Growable List<String> g,
      @Shrinkable List<String> s,
      @Replaceable List<String> r,
      @MaybeModifiable List<String> unknown,
      @Unmodifiable List<String> unmod) {

    // ============================================================
    // Identity on @Modifiable (G+Seq+S+R, on types that support all four capabilities)
    // ============================================================
    @Modifiable List<String> m1 = identity(mod); // OK: poly resolves to @Modifiable
    @MaybeModifiable List<String> m2 = identity(mod); // OK: positive capabilities <: tops
    @IteratorPolyMod List<String> m3 = identity(modIteratorPoly); // OK: preserves iterator poly
    // :: error: [assignment]
    @IteratorPolyMod List<String> m4 = identity(mod);

    // ============================================================
    // Identity on @Growable @Shrinkable (G+S; Seq=MaybeSeqGrowable, R=MaybeReplaceable)
    // ============================================================
    @Growable @Shrinkable List<String> gs1 = identity(gs); // OK
    // :: error: [assignment]
    @Modifiable List<String> gs2 = identity(gs); // Error: Seq/R=Maybe !<: positive
    @Growable List<String> gs3 = identity(gs); // OK
    @Shrinkable List<String> gs4 = identity(gs); // OK
    // :: error: [assignment]
    @Replaceable List<String> gs5 = identity(gs); // Error: MaybeR !<: Replaceable

    // ============================================================
    // Identity on @Growable @Replaceable (G+R; Seq=MaybeSeqGrowable, S=MaybeShrinkable)
    // ============================================================
    @Growable @Replaceable List<String> gr1 = identity(gr); // OK
    // :: error: [assignment]
    @Modifiable List<String> gr2 = identity(gr); // Error: Seq/S=Maybe !<: positive
    @Growable List<String> gr3 = identity(gr); // OK
    // :: error: [assignment]
    @Shrinkable List<String> gr4 = identity(gr); // Error: S=Maybe
    @Replaceable List<String> gr5 = identity(gr); // OK

    // ============================================================
    // Identity on @Shrinkable @Replaceable (S+R; G=MaybeGrowable, Seq=MaybeSeqGrowable)
    // ============================================================
    @Shrinkable @Replaceable List<String> sr1 = identity(sr); // OK
    // :: error: [assignment]
    @Modifiable List<String> sr2 = identity(sr); // Error: G/Seq=Maybe !<: positive
    // :: error: [assignment]
    @Growable List<String> sr3 = identity(sr); // Error: G=Maybe
    @Shrinkable List<String> sr4 = identity(sr); // OK
    @Replaceable List<String> sr5 = identity(sr); // OK

    // ============================================================
    // Identity on @Growable (G only; Seq=Maybe, S=Maybe, R=Maybe)
    // ============================================================
    @Growable List<String> g1 = identity(g); // OK
    // :: error: [assignment]
    @Growable @Shrinkable List<String> g2 = identity(g); // Error: S=Maybe
    // :: error: [assignment]
    @Modifiable List<String> g3 = identity(g); // Error: Seq/S/R=Maybe

    // ============================================================
    // Identity on @Shrinkable (S only; G=Maybe, Seq=Maybe, R=Maybe)
    // ============================================================
    @Shrinkable List<String> s1 = identity(s); // OK
    // :: error: [assignment]
    @Growable @Shrinkable List<String> s2 = identity(s); // Error: G=Maybe
    // :: error: [assignment]
    @Modifiable List<String> s3 = identity(s); // Error

    // ============================================================
    // Identity on @Replaceable (R only; G=Maybe, Seq=Maybe, S=Maybe)
    // ============================================================
    @Replaceable List<String> r1 = identity(r); // OK
    // :: error: [assignment]
    @Growable @Replaceable List<String> r2 = identity(r); // Error: G=Maybe
    // :: error: [assignment]
    @Modifiable List<String> r3 = identity(r); // Error

    // ============================================================
    // Identity on @MaybeModifiable (all tops)
    // ============================================================
    @MaybeModifiable List<String> u1 = identity(unknown); // OK
    // :: error: [assignment]
    @Modifiable List<String> u2 = identity(unknown); // Error
    // :: error: [assignment]
    @Growable List<String> u3 = identity(unknown); // Error

    // ============================================================
    // Identity on @Unmodifiable (negative capabilities, with structural weakening as needed)
    // ============================================================
    @Unmodifiable List<String> unmod1 = identity(unmod); // OK
    @MaybeModifiable List<String> unmod2 = identity(unmod); // OK
    // :: error: [assignment]
    @Modifiable List<String> unmod3 = identity(unmod); // Error
  }

  void testPolySeqGrowOnDeque(
      @Modifiable Deque<String> mod,
      @SeqGrowable Deque<String> seqGrow,
      @SeqUngrowable Deque<String> seqUngrow,
      @MaybeModifiable Deque<String> unknown,
      @Unmodifiable Deque<String> unmod) {

    @Modifiable Deque<String> m1 = identityDeque(mod); // OK: preserves all four positives.
    @SeqGrowable Deque<String> m2 = identityDeque(mod); // OK: keeps SeqGrow.

    @SeqGrowable Deque<String> sg1 = identityDeque(seqGrow); // OK
    // :: error: [assignment]
    @Modifiable Deque<String> sg2 = identityDeque(seqGrow); // Error: only SeqGrow is known.

    @SeqUngrowable Deque<String> usg1 = identityDeque(seqUngrow); // OK
    // :: error: [assignment]
    @SeqGrowable Deque<String> usg2 = identityDeque(seqUngrow); // Error: negative SeqGrow.

    @SeqUngrowable Deque<String> unmod1 = identityDeque(unmod); // OK
    // :: error: [assignment]
    @SeqGrowable Deque<String> unmod2 = identityDeque(unmod); // Error: negative SeqGrow.

    @MaybeModifiable Deque<String> unknown1 = identityDeque(unknown); // OK
    // :: error: [assignment]
    @SeqGrowable Deque<String> unknown2 = identityDeque(unknown); // Error: SeqGrow is unknown.
  }

  void testPolyOnEntry(
      Map.@Modifiable Entry<String, String> mod, Map.@SeqGrowable Entry<String, String> seqGrow) {

    // A Map.Entry has no grow, seq-grow, or shrink methods, so @PolyModifiable carries only the
    // replace capability, just as @Modifiable expands to only the replace capability.
    Map.@Replaceable Entry<String, String> e1 = identityEntry(mod); // OK

    Map.@MaybeSeqGrowable Entry<String, String> e2 = identityEntry(seqGrow); // OK
    // :: error: [assignment]
    Map.@SeqGrowable Entry<String, String> e3 = identityEntry(seqGrow);
  }
}
