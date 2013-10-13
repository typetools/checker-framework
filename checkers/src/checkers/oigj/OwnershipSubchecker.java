package checkers.oigj;

import checkers.basetype.BaseTypeChecker;
import checkers.oigj.quals.Dominator;
import checkers.oigj.quals.Modifier;
import checkers.oigj.quals.O;
import checkers.oigj.quals.World;
import checkers.quals.TypeQualifiers;
import checkers.source.SuppressWarningsKeys;

@TypeQualifiers({ Dominator.class, Modifier.class, World.class, O.class, OIGJMutabilityBottom.class })
@SuppressWarningsKeys({ "ownership", "oigj" })
public class OwnershipSubchecker extends BaseTypeChecker {
    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
