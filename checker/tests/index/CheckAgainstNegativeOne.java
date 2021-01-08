import org.checkerframework.checker.index.qual.IndexOrHigh;

public class CheckAgainstNegativeOne {

    public static String replaceString(String target, String oldStr, String newStr) {
        if (oldStr.equals("")) {
            throw new IllegalArgumentException();
        }

        StringBuffer result = new StringBuffer();
        @IndexOrHigh("target") int lastend = 0;
        int pos;
        while ((pos = target.indexOf(oldStr, lastend)) != -1) {
            result.append(target.substring(lastend, pos));
            result.append(newStr);
            lastend = pos + oldStr.length();
        }
        result.append(target.substring(lastend));
        return result.toString();
    }
}
