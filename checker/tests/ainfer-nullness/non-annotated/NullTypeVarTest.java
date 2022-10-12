// A test that the interaction between type variables and null types
// is handled correctly in WPI, based on the indentString variable
// in
// https://github.com/plume-lib/bcel-util/blob/master/src/main/java/org/plumelib/bcelutil/SimpleLog.java

import java.util.ArrayList;
import java.util.List;

public class NullTypeVarTest {

    // :: warning: assignment.type.incompatible
    private String indentString = null;

    private List<String> indentStrings;

    private final String INDENT_STR_ONE_LEVEL = "  ";

    public NullTypeVarTest() {
        indentStrings = new ArrayList<String>();
        indentStrings.add("");
    }

    private String getIndentString(int indentLevel) {
        if (indentString == null) {
            for (int i = indentStrings.size(); i <= indentLevel; i++) {
                indentStrings.add(indentStrings.get(i - 1) + INDENT_STR_ONE_LEVEL);
            }
            indentString = indentStrings.get(indentLevel);
        }
        return indentString;
    }
}
