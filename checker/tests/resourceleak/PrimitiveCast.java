import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;

import java.util.List;

public class PrimitiveCast {

    char foo(List<?> values) {
        for (Object o : values) {
            return (char) o;
        }
        return 'A';
    }

    char toChar1(@MustCall("hashCode") Character c) {
        return (char) c;
    }

    char toChar2(@MustCallUnknown Character c) {
        return (char) c;
    }

    char toChar3(@MustCall Character c) {
        return (char) c;
    }

    @MustCall("hashCode") Character toCharacter1(char c) {
        return (char) c;
    }

    @MustCallUnknown Character toCharacter2(char c) {
        return (char) c;
    }

    @MustCall Character toCharacter3(char c) {
        return (char) c;
    }
}
