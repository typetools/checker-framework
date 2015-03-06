package org.checkerframework.checker.oigj;

import org.checkerframework.checker.oigj.qual.AssignsFields;
import org.checkerframework.checker.oigj.qual.I;
import org.checkerframework.checker.oigj.qual.Immutable;
import org.checkerframework.checker.oigj.qual.Mutable;
import org.checkerframework.checker.oigj.qual.ReadOnly;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * <!-- TODO: reinstate once manual chapter exists: @checker_framework.manual #oigj-checker OIGJ Checker -->
 */
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
