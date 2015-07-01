package nullness;

import java.util.regex.Pattern;

class NullnessRegexWithErrors {
    String str = "(str";

    void context() {
        str = null;
        Pattern.compile("\\I");
    }
}
