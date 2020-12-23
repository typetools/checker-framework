// Test case for Issue panacekcz#16:
// https://github.com/panacekcz/checker-framework/issues/16

import java.util.StringTokenizer;

public class StringTokenizerMinLen {
    void test(String str, String delim, boolean returnDelims) {
        StringTokenizer st = new StringTokenizer(str, delim, returnDelims);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            char c = token.charAt(0);
        }
    }
}
