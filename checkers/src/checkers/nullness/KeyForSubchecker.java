package checkers.nullness;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.KeyFor;
import checkers.nullness.quals.KeyForBottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * TODO: doc
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifiers({ KeyFor.class, Unqualified.class, KeyForBottom.class})
public class KeyForSubchecker extends BaseTypeChecker {
    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
