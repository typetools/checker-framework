// Test to ensure that checkers can type-check big binary trees in an
// acceptable amount of time.  See comment on TreeAnnotator#visitBinary.

// Checkers may correctly issue errors, so suppress them.
@SuppressWarnings("all")
public class BigBinaryTrees {
  String string1;
  String string2;
  String string3;

  public void testStrings() {
    String s =
        getClass().getName()
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3
            + ",string1="
            + string1
            + ",string2="
            + string2
            + ",string3="
            + string3;
  }

  void test() {
    int i0 = 163;
    int i1 = 153;
    int i2 = 75;
    int i3 = -72;
    int i4 = 61;
    int i5 = 7;
    int i6 = 83;
    int i7 = -36;
    int i8 = -90;
    int i9 = -93;
    int i10 = 187;
    int i11 = -76;
    int i12 = -16;
    int i13 = -99;
    int i14 = 113;
    int i15 = 72;
    int i16 = 58;
    int i17 = -97;
    int i18 = 115;
    int i19 = -85;
    int i20 = 156;
    int i21 = -10;
    int i22 = -85;
    int i23 = 81;
    int i24 = 63;
    int i25 = -49;
    int i26 = 158;
    int i27 = 158;
    int i28 = 25;
    int i29 = 136;
    int i30 = -90;
    int i31 = 115;
    int i32 = 179;
    int i33 = 11;
    int i34 = -100;
    int i35 = 70;
    int i36 = -46;
    int i37 = -56;
    int i38 = 108;
    int i39 = -41;
    int i40 = 124;
    int i41 = -88;
    int i42 = 54;
    int i43 = 117;
    int i44 = -92;
    int i45 = 7;
    int i46 = -94;
    int i47 = 162;
    int i48 = -34;
    int i49 = 104;
    int i50 = 111;
    int i51 = -16;
    int i52 = 197;
    int i53 = -8;
    int i54 = 101;
    int i55 = 96;
    int i56 = 132;
    int i57 = -36;
    int i58 = 148;
    int i59 = 43;
    int i60 = -59;
    int i61 = 150;
    int i62 = 48;
    int i63 = 130;
    int i64 = 74;
    int i65 = -1;
    int i66 = 79;
    int i67 = 109;
    int i68 = -70;
    int i69 = 111;
    int i70 = 78;
    int i71 = 155;
    int i72 = 176;
    int i73 = 80;
    int i74 = 181;
    int i75 = 41;
    int i76 = -85;
    int i77 = 189;
    int i78 = 97;
    int i79 = 139;
    int i80 = 9;
    int i81 = 42;
    int i82 = -50;
    int i83 = 82;
    int i84 = -70;
    int i85 = 162;
    int i86 = -20;
    int i87 = 52;
    int i88 = -94;
    int i89 = 133;
    int i90 = 136;
    int i91 = 129;
    int i92 = -55;
    int i93 = 153;
    int i94 = 6;
    int i95 = -18;
    int i96 = 132;
    int i97 = 45;
    int i98 = 120;
    int i99 = 60;
    int result =
        i0 + i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13 + i14 + i15 + i16
            + i17 + i18 + i19 + i20 + i21 + i22 + i23 + i24 + i25 + i26 + i27 + i28 + i29 + i30
            + i31 + i32 + i33 + i34 + i35 + i36 + i37 + i38 + i39 + i40 + i41 + i42 + i43 + i44
            + i45 + i46 + i47 + i48 + i49 + i50 + i51 + i52 + i53 + i54 + i55 + i56 + i57 + i58
            + i59 + i60 + i61 + i62 + i63 + i64 + i65 + i66 + i67 + i68 + i69 + i70 + i71 + i72
            + i73 + i74 + i75 + i76 + i77 + i78 + i79 + i80 + i81 + i82 + i83 + i84 + i85 + i86
            + i87 + i88 + i89 + i90 + i91 + i92 + i93 + i94 + i95 + i96 + i97 + i98 + i99;
  }
}
