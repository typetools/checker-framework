package java.lang;
import checkers.javari.quals.*;

public interface CharSequence {

    int length(@ReadOnly CharSequence this);

    char charAt(@ReadOnly CharSequence this, int index);
    @PolyRead CharSequence subSequence(@PolyRead CharSequence this, int start, int end);
    public String toString(@ReadOnly CharSequence this);

}
