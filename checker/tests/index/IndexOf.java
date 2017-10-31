// Test case for issue #169: https://github.com/kelloggm/checker-framework/issues/169

// @skip-test until the issue is fixed

public class IndexOf {

    public static String m(String arg) {
        int split_pos = arg.indexOf(",-");
        if (split_pos == 0) {
            // Just discard the ',' if ",-" occurs at begining of string
            arg = arg.substring(1);
        }
        return arg;
    }
}
