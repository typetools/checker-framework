import org.checkerframework.common.value.qual.StringVal;

public class StringPolyValue {
    void stringValArrayLen(@StringVal({"a", "b", "c"}) String abc) {

        @StringVal({"a", "b", "c"}) String ns = new String(abc);
        @StringVal({"a", "b", "c"}) String ts = abc.toString();
        @StringVal({"a", "b", "c"}) String i = abc.intern();
        @StringVal({"a", "b", "c"}) String nstca = new String(abc.toCharArray());
        @StringVal({"a", "b", "c"}) String votca = String.valueOf(abc.toCharArray());
        @StringVal({"a", "b", "c"}) String cvotca = String.copyValueOf(abc.toCharArray());
    }
}
