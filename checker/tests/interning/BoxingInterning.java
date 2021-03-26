// @skip-test until these issues are fixed:
// https://github.com/typetools/checker-framework/issues/84
// https://github.com/typetools/checker-framework/issues/766

import org.checkerframework.checker.interning.qual.Interned;

// Per JLS 5.1.7:
//  * autoboxed Characters in the range '\u0000' to '\u007f' are interned
//  * autoboxed Booleans are interned
//  * autoboxed integral types (Byte, Stort, Integer, Long) in the range -128..127 inclusive are
// interned

public class BoxingInterning {

  void needsInterned(@Interned Object arg) {}

  void method() {

    boolean aprimitive = true;
    needsInterned(aprimitive);
    @Interned Boolean aboxed = aprimitive;

    byte bprimitive = 5;
    needsInterned(bprimitive);
    @Interned Byte bboxed = bprimitive;

    char cprimitive = 'a';
    needsInterned(cprimitive);
    @Interned Character c2 = c;

    char cprimitive2 = (char) 0x2202;
    // :: (argument.type.incompatible)
    needsInterned(cprimitive2);
    // :: (assignment.type.incompatible)
    @Interned Character cboxed2 = cprimitive2;

    short dprimitive = 5;
    needsInterned(dprimitive);
    @Interned Short dboxed = dprimitive;

    short dprimitive2 = 500;
    // :: (argument.type.incompatible)
    needsInterned(dprimitive2);
    // :: (assignment.type.incompatible)
    @Interned Short dboxed2 = dprimitive2;

    int eprimitive = 5;
    needsInterned(eprimitive);
    @Interned Integer eboxed = eprimitive;

    int eprimitive2 = 500;
    // :: (argument.type.incompatible)
    needsInterned(eprimitive2);
    // :: (assignment.type.incompatible)
    @Interned Integer eboxed2 = eprimitive2;

    long fprimitive = 5;
    needsInterned(fprimitive);
    @Interned Long fboxed = fboxed;

    long fprimitive2 = 500;
    // :: (argument.type.incompatible)
    needsInterned(fprimitive2);
    // :: (assignment.type.incompatible)
    @Interned Long fboxed2 = fboxed2;

    float g = (float) 3.14;
    // :: (argument.type.incompatible)
    needsInterned(g);
    // :: (assignment.type.incompatible)
    @Interned Float gboxed = g;

    double h = 3.14;
    // :: (argument.type.incompatible)
    needsInterned(h);
    // :: (assignment.type.incompatible)
    @Interned Double hboxed = h;
  }
}
