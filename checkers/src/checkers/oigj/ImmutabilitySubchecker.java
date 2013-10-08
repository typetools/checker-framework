package checkers.oigj;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.AssignsFields;
import checkers.oigj.quals.I;
import checkers.oigj.quals.Immutable;
import checkers.oigj.quals.Mutable;
import checkers.oigj.quals.ReadOnly;
import checkers.quals.TypeQualifiers;
import checkers.source.SuppressWarningsKeys;

@TypeQualifiers({ ReadOnly.class, Mutable.class, Immutable.class, I.class,
    AssignsFields.class, OIGJMutabilityBottom.class })
@SuppressWarningsKeys({ "immutability", "oigj" })
public class ImmutabilitySubchecker extends BaseTypeChecker {
    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
