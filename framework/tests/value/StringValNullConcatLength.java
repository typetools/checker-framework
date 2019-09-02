import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.StringVal;

public class StringValNullConcatLength {
    @StringVal("a") String string1;

    @StringVal("b") String string2;
    // "ab", "anull", "nullb", "nullnull"
    @ArrayLen({2, 5, 8}) String string3 = string1 + string2;
}
