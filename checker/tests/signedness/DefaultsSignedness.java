import org.checkerframework.checker.signedness.qual.*;

public class DefaultsSignedness {

    public void ConstantTest() {

        // Test bytes with literal values
        @SignednessGlb byte conByte;
        @SignednessBottom byte botByte;

        byte testByte = 0;

        conByte = testByte;

        // :: error: (assignment.type.incompatible)
        botByte = testByte;

        // Test shorts with literal values
        @SignednessGlb short conShort;
        @SignednessBottom short botShort;

        short testShort = 128;

        conShort = testShort;

        // :: error: (assignment.type.incompatible)
        botShort = testShort;

        // Test ints with literal values
        @SignednessGlb int conInt;
        @SignednessBottom int botInt;

        int testInt = 32768;

        conInt = testInt;

        // :: error: (assignment.type.incompatible)
        botInt = testInt;

        // Test longs with literal values
        @SignednessGlb long conLong;
        @SignednessBottom long botLong;

        long testLong = 2147483648L;

        conLong = testLong;

        // :: error: (assignment.type.incompatible)
        botLong = testLong;

        // Test chars with literal values
        @SignednessGlb char conChar;
        @SignednessBottom char botChar;

        char testChar = 'a';

        conChar = testChar;

        // :: error: (assignment.type.incompatible)
        botChar = testChar;
    }

    public void SignedTest(
            byte testByte,
            short testShort,
            int testInt,
            long testLong,
            float testFloat,
            double testDouble,
            char testChar,
            boolean testBool,
            Byte testBoxedByte,
            Short testBoxedShort,
            Integer testBoxedInteger,
            Long testBoxedLong) {

        // Test bytes
        @Signed byte sinByte;
        @SignednessGlb byte conByte;

        sinByte = testByte;

        // :: error: (assignment.type.incompatible)
        conByte = testByte;

        // Test shorts
        @Signed short sinShort;
        @SignednessGlb short conShort;

        sinShort = testShort;

        // :: error: (assignment.type.incompatible)
        conShort = testShort;

        // Test ints
        @Signed int sinInt;
        @SignednessGlb int conInt;

        sinInt = testInt;

        // :: error: (assignment.type.incompatible)
        conInt = testInt;

        // Test longs
        @Signed long sinLong;
        @SignednessGlb long conLong;

        sinLong = testLong;

        // :: error: (assignment.type.incompatible)
        conLong = testLong;

        // Test floats
        // :: error: (anno.on.irrelevant)
        @Signed float sinFloat;

        sinFloat = testFloat;

        // Test doubles
        // :: error: (anno.on.irrelevant)
        @Signed double sinDouble;

        sinDouble = testDouble;

        /*
        // Test boxed bytes
        @Signed Byte sinBoxedByte;
        @SignednessGlb Byte conBoxedByte;

        sinBoxedByte = testBoxedByte;

        //// :: error: (assignment.type.incompatible)
        conBoxedByte = testBoxedByte;

        // Test boxed shorts
        @Signed Short sinBoxedShort;
        @SignednessGlb Short conBoxedShort;

        sinBoxedShort = testBoxedShort;

        //// :: error: (assignment.type.incompatible)
        conBoxedShort = testBoxedShort;

        // Test boxed Integers
        @Signed Integer sinBoxedInteger;
        @SignednessGlb Integer conBoxedInteger;

        sinBoxedInteger = testBoxedInteger;

        //// :: error: (assignment.type.incompatible)
        conBoxedInteger = testBoxedInteger;

        // Test boxed Longs
        @Signed Long sinBoxedLong;
        @SignednessGlb Long conBoxedLong;

        sinBoxedLong = testBoxedLong;

        //// :: error: (assignment.type.incompatible)
        conBoxedLong = testBoxedLong;
        */
    }

    public void SignednessBottom() {

        @SignednessBottom Object botObj;

        Object testObj = null;

        botObj = testObj;
    }

    public void UnknownSignedness(Object testObj, @Unsigned int unsigned, @Signed int signed) {

        @UnknownSignedness Object unkObj;
        @Unsigned Object unsinObj;

        unkObj = testObj;

        // :: error: (assignment)
        unsinObj = testObj;
    }

    public void booleanProblem(@Unsigned int unsigned, @Signed int signed) {
        boolean testBool = unsigned == 1 || signed > 1;
    }
}
