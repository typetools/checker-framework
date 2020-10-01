// A test for the @EnumVal annotation.

import org.checkerframework.common.value.qual.*;
import org.checkerframework.common.value.qual.StringVal;

class EnumConstants {
    enum MyEnum {
        VALUE,
        OTHER_VALUE,
        THIRD_VALUE
    }

    static void subtyping1(@EnumVal("VALUE") MyEnum value) {
        @EnumVal("VALUE") MyEnum value2 = value;
        // :: error: (assignment.type.incompatible)
        @EnumVal("OTHER_VALUE") MyEnum value3 = value;
        @UnknownVal MyEnum value4 = value;
        @EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum value5 = value;
    }

    static void subtyping2(@EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum value) {
        // :: error: (assignment.type.incompatible)
        @EnumVal("VALUE") MyEnum value2 = value;
        // :: error: (assignment.type.incompatible)
        @EnumVal("OTHER_VALUE") MyEnum value3 = value;
        @UnknownVal MyEnum value4 = value;
        @EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum value5 = value;
        @EnumVal({"VALUE", "OTHER_VALUE", "THIRD_VALUE"}) MyEnum value6 = value;
    }

    static void enumConstants() {
        @EnumVal("VALUE") MyEnum v1 = MyEnum.VALUE;
        @EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum v2 = MyEnum.VALUE;
        // :: error: (assignment.type.incompatible)
        @EnumVal("OTHER_VALUE") MyEnum v3 = MyEnum.VALUE;
    }

    static void enumToString() {
        @EnumVal("VALUE") MyEnum v1 = MyEnum.VALUE;
        @StringVal("VALUE") String s1 = v1.toString();
    }

    // These are just paranoia based on the implementation strategy for enum constant defaulting.
    static void nonConstantEnum(MyEnum m) {
        // :: error: (assignment.type.incompatible)
        @EnumVal("m") MyEnum m2 = m;
        // :: error: (assignment.type.incompatible)
        @EnumVal("m3") MyEnum m3 = m;
    }

    static void enums(@EnumVal("VALUE") MyEnum... enums) {}

    static void testEnums() {
        enums();
        enums(MyEnum.VALUE);
        // :: error: (argument.type.incompatible)
        enums(MyEnum.OTHER_VALUE);
    }
}
