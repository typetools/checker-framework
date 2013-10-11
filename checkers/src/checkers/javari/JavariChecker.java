package checkers.javari;

import checkers.basetype.BaseTypeChecker;
import checkers.javari.quals.Mutable;
import checkers.javari.quals.PolyRead;
import checkers.javari.quals.QReadOnly;
import checkers.javari.quals.ReadOnly;
import checkers.javari.quals.ThisMutable;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;

/**
 * An annotation processor that checks a program's use of the Javari
 * type annotations ({@code @ReadOnly}, {@code @Mutable},
 * {@code @Assignable}, {@code @PolyRead} and {@code @QReadOnly}).
 *
 * @checker.framework.manual #javari-checker Javari Checker
 */
@TypeQualifiers( { ReadOnly.class, ThisMutable.class, Mutable.class,
    PolyRead.class, QReadOnly.class, PolyAll.class })
public class JavariChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
