// test case for https://github.com/typetools/checker-framework/issues/3442

public class Tempvars {
    static {
        int i = 0;
        i++;
    }
}
