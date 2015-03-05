package org.checkerframework.checker.oigj;

import org.checkerframework.checker.oigj.qual.Dominator;
import org.checkerframework.checker.oigj.qual.Modifier;
import org.checkerframework.checker.oigj.qual.O;
import org.checkerframework.checker.oigj.qual.World;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * <!-- TODO: reinstate once manual chapter exists: @checker_framework.manual #oigj-checker OIGJ Checker -->
 */
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
