import java.util.Deque;
import org.checkerframework.checker.modifiability.qual.BottomGrowable;
import org.checkerframework.checker.modifiability.qual.BottomReplaceable;
import org.checkerframework.checker.modifiability.qual.BottomSeqGrowable;
import org.checkerframework.checker.modifiability.qual.BottomShrinkable;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeGrowable;
import org.checkerframework.checker.modifiability.qual.MaybeModifiable;
import org.checkerframework.checker.modifiability.qual.MaybeReplaceable;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Ungrowable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;
import org.checkerframework.checker.modifiability.qual.Unreplaceable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;

/**
 * Tests the four independent 4-element capability lattices of the Modifiability Checker.
 *
 * <p>Each hierarchy is: Maybe* (top/default), two incomparable siblings (*able and Un*able), and
 * Bottom* (bottom).
 *
 * <p>An annotated type carries one qualifier per hierarchy. Assignment is valid if and only if the
 * RHS is a subtype in every hierarchy simultaneously.
 */
class HierarchyTest {

  void testGrow(
      @Growable Object g,
      @Ungrowable Object ug,
      @MaybeGrowable Object u,
      @BottomGrowable Object b) {
    @MaybeGrowable Object u1 = g;
    @MaybeGrowable Object u2 = ug;
    @MaybeGrowable Object u3 = b;

    @Growable Object g1 = b;
    @Ungrowable Object ug1 = b;

    // :: error: [assignment]
    @Growable Object g2 = ug;
    // :: error: [assignment]
    @Ungrowable Object ug2 = g;
    // :: error: [assignment]
    @Growable Object g3 = u;
    // :: error: [assignment]
    @Ungrowable Object ug3 = u;
    // :: error: [assignment]
    @BottomGrowable Object b1 = g;
    // :: error: [assignment]
    @BottomGrowable Object b2 = ug;
    // :: error: [assignment]
    @BottomGrowable Object b3 = u;
  }

  void testShrink(
      @Shrinkable Object s,
      @Unshrinkable Object us,
      @MaybeShrinkable Object u,
      @BottomShrinkable Object b) {
    @MaybeShrinkable Object u1 = s;
    @MaybeShrinkable Object u2 = us;
    @MaybeShrinkable Object u3 = b;

    @Shrinkable Object s1 = b;
    @Unshrinkable Object us1 = b;

    // :: error: [assignment]
    @Shrinkable Object s2 = us;
    // :: error: [assignment]
    @Unshrinkable Object us2 = s;
    // :: error: [assignment]
    @Shrinkable Object s3 = u;
    // :: error: [assignment]
    @Unshrinkable Object us3 = u;
  }

  void testSeqGrow(
      @SeqGrowable Object sg,
      @SeqUngrowable Object usg,
      @MaybeSeqGrowable Object u,
      @BottomSeqGrowable Object b) {
    @MaybeSeqGrowable Object u1 = sg;
    @MaybeSeqGrowable Object u2 = usg;
    @MaybeSeqGrowable Object u3 = b;

    @SeqGrowable Object sg1 = b;
    @SeqUngrowable Object usg1 = b;

    // :: error: [assignment]
    @SeqGrowable Object sg2 = usg;
    // :: error: [assignment]
    @SeqUngrowable Object usg2 = sg;
    // :: error: [assignment]
    @SeqGrowable Object sg3 = u;
    // :: error: [assignment]
    @SeqUngrowable Object usg3 = u;
    // :: error: [assignment]
    @BottomSeqGrowable Object b1 = sg;
    // :: error: [assignment]
    @BottomSeqGrowable Object b2 = usg;
    // :: error: [assignment]
    @BottomSeqGrowable Object b3 = u;
  }

  void testReplace(
      @Replaceable Object r,
      @Unreplaceable Object ur,
      @MaybeReplaceable Object u,
      @BottomReplaceable Object b) {
    @MaybeReplaceable Object u1 = r;
    @MaybeReplaceable Object u2 = ur;
    @MaybeReplaceable Object u3 = b;

    @Replaceable Object r1 = b;
    @Unreplaceable Object ur1 = b;

    // :: error: [assignment]
    @Replaceable Object r2 = ur;
    // :: error: [assignment]
    @Unreplaceable Object ur2 = r;
    // :: error: [assignment]
    @Replaceable Object r3 = u;
    // :: error: [assignment]
    @Unreplaceable Object ur3 = u;
  }

  void testSeqGrowCombinations(
      @Growable @SeqGrowable Object gsg,
      @Growable Object g,
      @SeqGrowable Object sg,
      @Ungrowable @SeqUngrowable Object none,
      @MaybeGrowable @MaybeSeqGrowable Object unknown) {

    @Growable Object gv1 = gsg;
    @SeqGrowable Object sgv1 = gsg;

    @Growable Object gv2 = g;
    // :: error: [assignment]
    @SeqGrowable Object sgv2 = g;

    // :: error: [assignment]
    @Growable Object gv3 = sg;
    @SeqGrowable Object sgv3 = sg;

    // :: error: [assignment]
    @Growable Object gv4 = none;
    // :: error: [assignment]
    @SeqGrowable Object sgv4 = none;

    // :: error: [assignment]
    @Growable Object gv5 = unknown;
    // :: error: [assignment]
    @SeqGrowable Object sgv5 = unknown;

    @MaybeGrowable @MaybeSeqGrowable Object tv1 = gsg;
    @MaybeGrowable @MaybeSeqGrowable Object tv2 = g;
    @MaybeGrowable @MaybeSeqGrowable Object tv3 = sg;
    @MaybeGrowable @MaybeSeqGrowable Object tv4 = none;
    @MaybeGrowable @MaybeSeqGrowable Object tv5 = unknown;
  }

  void testCombinations(
      @Growable @Shrinkable @Replaceable Object gsr,
      @Growable @Shrinkable Object gs,
      @Growable @Replaceable Object gr,
      @Shrinkable @Replaceable Object sr,
      @Growable Object g,
      @Shrinkable Object s,
      @Replaceable Object r,
      @Ungrowable @Unshrinkable @Unreplaceable Object none,
      @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object unknown) {

    @Growable Object gv1 = gsr;
    @Growable Object gv2 = gs;
    @Growable Object gv3 = gr;
    // :: error: [assignment]
    @Growable Object gv4 = sr;
    // :: error: [assignment]
    @Growable Object gv5 = s;
    // :: error: [assignment]
    @Growable Object gv6 = r;
    // :: error: [assignment]
    @Growable Object gv7 = none;
    // :: error: [assignment]
    @Growable Object gv8 = unknown;

    @Shrinkable Object sv1 = gsr;
    @Shrinkable Object sv2 = gs;
    // :: error: [assignment]
    @Shrinkable Object sv3 = gr;
    @Shrinkable Object sv4 = sr;
    // :: error: [assignment]
    @Shrinkable Object sv5 = g;
    // :: error: [assignment]
    @Shrinkable Object sv6 = r;
    // :: error: [assignment]
    @Shrinkable Object sv7 = none;
    // :: error: [assignment]
    @Shrinkable Object sv8 = unknown;

    @Replaceable Object rv1 = gsr;
    // :: error: [assignment]
    @Replaceable Object rv2 = gs;
    @Replaceable Object rv3 = gr;
    @Replaceable Object rv4 = sr;
    // :: error: [assignment]
    @Replaceable Object rv5 = g;
    // :: error: [assignment]
    @Replaceable Object rv6 = s;
    // :: error: [assignment]
    @Replaceable Object rv7 = none;
    // :: error: [assignment]
    @Replaceable Object rv8 = unknown;

    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv1 = gsr;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv2 = gs;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv3 = gr;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv4 = sr;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv5 = g;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv6 = s;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv7 = r;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv8 = none;
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable Object tv9 = unknown;
  }

  void testAliases(
      @Modifiable Object mod, @Unmodifiable Object unmod, @MaybeModifiable Object unknown) {

    @Growable Object g1 = mod;
    @Shrinkable Object s1 = mod;
    @Replaceable Object r1 = mod;

    @Ungrowable Object ug1 = unmod;
    @Unshrinkable Object us1 = unmod;
    @Unreplaceable Object ur1 = unmod;

    @MaybeGrowable Object ug2 = unknown;
    @MaybeSeqGrowable Object usg2 = unknown;
    @MaybeShrinkable Object us2 = unknown;
    @MaybeReplaceable Object ur2 = unknown;

    @MaybeModifiable Object unknown1 = mod;
    @MaybeModifiable Object unknown2 = unmod;

    // :: error: [assignment]
    @Unmodifiable Object unmod1 = mod;
    // :: error: [assignment]
    @Unmodifiable Object unmod2 = unknown;
    // :: error: [assignment]
    @Growable Object g2 = unmod;
    // @Object cannot structurally support sequenced-grow operations, so whole-modifiability aliases
    // use @MaybeSeqGrowable for their SeqGrow component.
    @MaybeSeqGrowable Object maybeSeqGrowable1 = mod;
    @MaybeSeqGrowable Object maybeSeqGrowable2 = unmod;
    // :: error: [assignment]
    @SeqGrowable Object sg1 = mod;
    // :: error: [assignment]
    @SeqUngrowable Object usg1 = unmod;
    // :: error: [assignment]
    @Modifiable Object mod1 = unmod;
  }

  void testAliasesOnSeqGrowableType(
      @Modifiable Deque<String> mod,
      @Unmodifiable Deque<String> unmod,
      @MaybeModifiable Deque<String> unknown) {

    @SeqGrowable Deque<String> sg1 = mod;
    @SeqUngrowable Deque<String> usg1 = unmod;
    @MaybeSeqGrowable Deque<String> msg1 = unknown;

    @MaybeSeqGrowable Deque<String> msg2 = mod;
    @MaybeSeqGrowable Deque<String> msg3 = unmod;

    // :: error: [assignment]
    @SeqUngrowable Deque<String> usg2 = mod;
    // :: error: [assignment]
    @SeqGrowable Deque<String> sg2 = unmod;
    // :: error: [assignment]
    @SeqGrowable Deque<String> sg3 = unknown;
  }
}
