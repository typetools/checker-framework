import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.SignednessGlb;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

public class ValueIntegration {
    public void ByteValRules(
            @IntVal({0, 127}) byte c,
            @IntVal({128, 255}) byte upure,
            @IntVal({0, 128}) byte umixed, // 128 is another way to write -128
            @IntVal({-128, -1}) byte spure,
            @IntVal({-1, 127}) byte smixed,
            @IntVal({-128, 0, 128}) byte bmixed) {
        @Signed byte stest;
        @SignednessGlb byte gtest;
        @SignedPositive byte ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxByteValRules(
            @IntVal({0, 127}) Byte c,
            @IntVal({128, 255}) Byte upure,
            @IntVal({0, 128}) Byte umixed, // 128 is another way to write -128
            @IntVal({-128, -1}) Byte spure,
            @IntVal({-1, 127}) Byte smixed,
            @IntVal({-128, 0, 128}) Byte bmixed) {
        @Signed Byte stest;
        @SignednessGlb Byte gtest;
        @SignedPositive Byte ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void CharValRules(
            @IntVal({0, 127}) char c,
            @IntVal({128, 255}) char upure,
            @IntVal({0, 128}) char umixed,
            @IntVal({-128, -1}) char spure,
            @IntVal({-1, 127}) char smixed,
            @IntVal({-128, 0, 128}) char bmixed) {
        @Signed char stest;
        @SignednessGlb char gtest;
        @SignedPositive char ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxCharValRules(
            @IntVal({0, 127}) Character c,
            @IntVal({128, 255}) Character upure,
            @IntVal({0, 128}) Character umixed,
            @IntVal({-128, -1}) Character spure,
            @IntVal({-1, 127}) Character smixed,
            @IntVal({-128, 0, 128}) Character bmixed) {
        @Signed Character stest;
        @SignednessGlb Character gtest;
        @SignedPositive Character ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void ShortValRules(
            @IntVal({0, 32767}) short c,
            @IntVal({32768, 65535}) short upure,
            @IntVal({0, 32768}) short umixed,
            @IntVal({-32768, -1}) short spure,
            @IntVal({-1, 32767}) short smixed,
            @IntVal({-32768, 0, 32768}) short bmixed) {
        @Signed short stest;
        @SignednessGlb short gtest;
        @SignedPositive short ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxShortValRules(
            @IntVal({0, 32767}) Short c,
            @IntVal({32768, 65535}) Short upure,
            @IntVal({0, 32768}) Short umixed,
            @IntVal({-32768, -1}) Short spure,
            @IntVal({-1, 32767}) Short smixed,
            @IntVal({-32768, 0, 32768}) Short bmixed) {
        @Signed Short stest;
        @SignednessGlb Short gtest;
        @SignedPositive Short ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void IntValRules(
            @IntVal({0, 2147483647}) int c,
            @IntVal({2147483648L, 4294967295L}) int upure,
            @IntVal({0, 2147483648L}) int umixed,
            @IntVal({-2147483648, -1}) int spure,
            @IntVal({-1, 2147483647}) int smixed,
            @IntVal({-2147483648, 0, 2147483648L}) int bmixed) {
        @Signed int stest;
        @SignednessGlb int gtest;
        @SignedPositive int ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxIntValRules(
            @IntVal({0, 2147483647}) Integer c,
            @IntVal({2147483648L, 4294967295L}) Integer upure,
            @IntVal({0, 2147483648L}) Integer umixed,
            @IntVal({-2147483648, -1}) Integer spure,
            @IntVal({-1, 2147483647}) Integer smixed,
            @IntVal({-2147483648, 0, 2147483648L}) Integer bmixed) {
        @Signed Integer stest;
        @SignednessGlb Integer gtest;
        @SignedPositive Integer ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void LongValRules(
            @IntVal({0, Long.MAX_VALUE}) long c,
            @IntVal({Long.MIN_VALUE, -1}) long spure,
            @IntVal({-1, Long.MAX_VALUE}) long smixed,
            @IntVal({Long.MIN_VALUE, 0, Long.MAX_VALUE}) long bmixed) {
        @Signed long stest;
        @SignednessGlb long gtest;
        @SignedPositive long ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxLongValRules(
            @IntVal({0, Long.MAX_VALUE}) Long c,
            @IntVal({Long.MIN_VALUE, -1}) Long spure,
            @IntVal({-1, Long.MAX_VALUE}) Long smixed,
            @IntVal({Long.MIN_VALUE, 0, Long.MAX_VALUE}) Long bmixed) {
        @Signed Long stest;
        @SignednessGlb Long gtest;
        @SignedPositive Long ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void ByteRangeRules(
            @IntRange(from = 0, to = 127) byte c,
            @NonNegative byte nnc,
            @Positive byte pc,
            @IntRange(from = 128, to = 255) byte upure,
            @IntRange(from = 0, to = 128) byte umixed,
            @IntRange(from = -128, to = -1) byte spure,
            @IntRange(from = -1, to = 127) byte smixed,
            @IntRange(from = -128, to = 128) byte bmixed) {
        @Signed byte stest;
        @SignednessGlb byte gtest;
        @SignedPositive byte ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = nnc;
        gtest = nnc;
        ptest = nnc;

        stest = pc;
        gtest = pc;
        ptest = pc;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxByteRangeRules(
            @IntRange(from = 0, to = 127) Byte c,
            @NonNegative Byte nnc,
            @Positive Byte pc,
            @IntRange(from = 128, to = 255) Byte upure,
            @IntRange(from = 0, to = 128) Byte umixed,
            @IntRange(from = -128, to = -1) Byte spure,
            @IntRange(from = -1, to = 127) Byte smixed,
            @IntRange(from = -128, to = 128) Byte bmixed) {
        @Signed Byte stest;
        @SignednessGlb Byte gtest;
        @SignedPositive Byte ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void CharRangeRules(
            @IntRange(from = 0, to = 127) char c,
            @NonNegative char nnc,
            @Positive char pc,
            @IntRange(from = 128, to = 255) char upure,
            @IntRange(from = 0, to = 128) char umixed,
            @IntRange(from = -128, to = -1) char spure,
            @IntRange(from = -1, to = 127) char smixed,
            @IntRange(from = -128, to = 128) char bmixed) {
        @Signed char stest;
        @SignednessGlb char gtest;
        @SignedPositive char ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = nnc;
        gtest = nnc;
        ptest = nnc;

        stest = pc;
        gtest = pc;
        ptest = pc;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxCharRangeRules(
            @IntRange(from = 0, to = 127) Character c,
            @NonNegative Character nnc,
            @Positive Character pc,
            @IntRange(from = 128, to = 255) Character upure,
            @IntRange(from = 0, to = 128) Character umixed,
            @IntRange(from = -128, to = -1) Character spure,
            @IntRange(from = -1, to = 127) Character smixed,
            @IntRange(from = -128, to = 128) Character bmixed) {
        @Signed Character stest;
        @SignednessGlb Character gtest;
        @SignedPositive Character ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void ShortRangeRules(
            @IntRange(from = 0, to = 32767) short c,
            @NonNegative short nnc,
            @Positive short pc,
            @IntRange(from = 32768, to = 65535) short upure,
            @IntRange(from = 0, to = 32768) short umixed,
            @IntRange(from = -32768, to = -1) short spure,
            @IntRange(from = -1, to = 32767) short smixed,
            @IntRange(from = -32768, to = 32768) short bmixed) {
        @Signed short stest;
        @SignednessGlb short gtest;
        @SignedPositive short ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = nnc;
        gtest = nnc;
        ptest = nnc;

        stest = pc;
        gtest = pc;
        ptest = pc;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxShortRangeRules(
            @IntRange(from = 0, to = 32767) Short c,
            @NonNegative Short nnc,
            @Positive Short pc,
            @IntRange(from = 32768, to = 65535) Short upure,
            @IntRange(from = 0, to = 32768) Short umixed,
            @IntRange(from = -32768, to = -1) Short spure,
            @IntRange(from = -1, to = 32767) Short smixed,
            @IntRange(from = -32768, to = 32768) Short bmixed) {
        @Signed Short stest;
        @SignednessGlb Short gtest;
        @SignedPositive Short ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void IntRangeRules(
            @IntRange(from = 0, to = 2147483647) int c,
            @NonNegative int nnc,
            @Positive int pc,
            @IntRange(from = 2147483648L, to = 4294967295L) int upure,
            @IntRange(from = 0, to = 2147483648L) int umixed,
            @IntRange(from = -2147483648, to = -1) int spure,
            @IntRange(from = -1, to = 2147483647) int smixed,
            @IntRange(from = -2147483648, to = 2147483648L) int bmixed) {
        @Signed int stest;
        @SignednessGlb int gtest;
        @SignedPositive int ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = nnc;
        gtest = nnc;
        ptest = nnc;

        stest = pc;
        gtest = pc;
        ptest = pc;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxIntRangeRules(
            @IntRange(from = 0, to = 2147483647) Integer c,
            @NonNegative Integer nnc,
            @Positive Integer pc,
            @IntRange(from = 2147483648L, to = 4294967295L) Integer upure,
            @IntRange(from = 0, to = 2147483648L) Integer umixed,
            @IntRange(from = -2147483648, to = -1) Integer spure,
            @IntRange(from = -1, to = 2147483647) Integer smixed,
            @IntRange(from = -2147483648, to = 2147483648L) Integer bmixed) {
        @Signed Integer stest;
        @SignednessGlb Integer gtest;
        @SignedPositive Integer ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = upure;
        // :: error: (assignment.type.incompatible)
        gtest = upure;
        // :: error: (assignment.type.incompatible)
        ptest = upure;

        stest = umixed;
        // :: error: (assignment.type.incompatible)
        gtest = umixed;
        // :: error: (assignment.type.incompatible)
        ptest = umixed;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void LongRangeRules(
            @IntRange(from = 0, to = Long.MAX_VALUE) long c,
            @NonNegative long nnc,
            @Positive long pc,
            @IntRange(from = Long.MIN_VALUE, to = -1) long spure,
            @IntRange(from = -1, to = Long.MAX_VALUE) long smixed,
            @IntRange(from = Long.MIN_VALUE, to = Long.MAX_VALUE) long bmixed) {
        @Signed long stest;
        @SignednessGlb long gtest;
        @SignedPositive long ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = nnc;
        gtest = nnc;
        ptest = nnc;

        stest = pc;
        gtest = pc;
        ptest = pc;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }

    public void boxLongRangeRules(
            @IntRange(from = 0, to = Long.MAX_VALUE) Long c,
            @IntRange(from = Long.MIN_VALUE, to = -1) Long spure,
            @IntRange(from = -1, to = Long.MAX_VALUE) Long smixed,
            @IntRange(from = Long.MIN_VALUE, to = Long.MAX_VALUE) Long bmixed) {
        @Signed Long stest;
        @SignednessGlb Long gtest;
        @SignedPositive Long ptest;

        stest = c;
        gtest = c;
        ptest = c;

        stest = spure;
        // :: error: (assignment.type.incompatible)
        gtest = spure;
        // :: error: (assignment.type.incompatible)
        ptest = spure;

        stest = smixed;
        // :: error: (assignment.type.incompatible)
        gtest = smixed;
        // :: error: (assignment.type.incompatible)
        ptest = smixed;

        stest = bmixed;
        // :: error: (assignment.type.incompatible)
        gtest = bmixed;
        // :: error: (assignment.type.incompatible)
        ptest = bmixed;
    }
}
