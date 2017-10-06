// Tests suport for index annotations applied to CharSequence and related indices.
import java.io.IOException;
import java.io.StringWriter;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.StringVal;

public class CharSequenceTest {
    // Tests that minlen is correctly applied to CharSequence assigned from String, but not StringBuilder
    void minLenCharSequence() {
        @MinLen(10) CharSequence str = "0123456789";
        //:: error: (assignment.type.incompatible)
        @MinLen(10) CharSequence sb = new StringBuilder("0123456789");
    }
    // Tests the subSequence method
    void testSubSequence() {
        // Local variable used because of https://github.com/kelloggm/checker-framework/issues/165
        String str = "0123456789";
        str.subSequence(5, 8);
        //:: error: (argument.type.incompatible)
        str.subSequence(5, 13);
    }

    // Dummy method that takes a CharSequence and its index
    void sink(CharSequence cs, @IndexOrHigh("#1") int i) {}

    // Tests passing sequences as CharSequence
    void argumentPassing() {
        String s = "0123456789";
        sink(s, 8);
        StringBuilder sb = new StringBuilder("0123456789");
        //:: error: (argument.type.incompatible)
        sink(sb, 8);
    }
    // Tests forwardning sequences as CharSequence
    void agumentForwarding(String s, @IndexOrHigh("#1") int i) {
        sink(s, i);
    }

    // Tests concatenation of CharSequence and String
    void concat() {
        CharSequence a = "a";
        @StringVal("ab") CharSequence ab = a + "b";
        sink(ab, 2);
    }

    // Tests that length retrieved from CharSequence cannot be used as an index
    void getLength(CharSequence cs, int i) {
        if (i >= 0 && i < cs.length()) {
            //:: error: (argument.type.incompatible)
            cs.charAt(i);
        }

        //:: error: (assignment.type.incompatible)
        @IndexOrHigh("cs") int l = cs.length();
    }

    void testCharAt(CharSequence cs, int i, @IndexFor("#1") int j) {
        cs.charAt(j);
        cs.subSequence(j, j);
        //:: error: (argument.type.incompatible)
        cs.charAt(i);
        //:: error: (argument.type.incompatible)
        cs.subSequence(i, j);
    }

    void testAppend(Appendable app, CharSequence cs, @IndexFor("#2") int i) throws IOException {
        app.append(cs, i, i);
        //:: error: (argument.type.incompatible)
        app.append(cs, 1, 2);
    }

    void testAppend(StringWriter app, CharSequence cs, @IndexFor("#2") int i) throws IOException {
        app.append(cs, i, i);
        //:: error: (argument.type.incompatible)
        app.append(cs, 1, 2);
    }
}
