// Test for https://github.com/panacekcz/checker-framework/issues/4

// @skip-test The upper bound checker does not handle offsets precisely
// enough to prove that lastend is @IndexOrHigh("target").

class CheckAgainstNegativeOne {

    public static String replaceString(String target, String oldStr, String newStr) {
        if (oldStr.equals("")) {
            throw new IllegalArgumentException();
        }

        StringBuffer result = new StringBuffer();
        int lastend = 0;
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
