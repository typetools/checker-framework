package java.lang;
import checkers.javari.quals.*;

public interface CharSequence {

    int length() @ReadOnly;

    char charAt(int index) @ReadOnly;
    @PolyRead CharSequence subSequence(int start, int end) @PolyRead;
    public String toString() @ReadOnly;

}
