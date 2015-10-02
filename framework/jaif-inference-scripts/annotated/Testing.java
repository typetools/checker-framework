import org.checkerframework.common.value.qual.StringVal;
public class Testing {

    @StringVal({"asd"})
    String s = null;
    public void m() {
        s = "asd";
    }

}
