package org.checkerframework.common.accumulation;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;

/**
 * An accumulation checker is one that accumulates some property: method calls, keys into maps, etc.
 * All accumulation analyses share some common features: their type systems are similar in
 * structure, they need to reason about fluent APIs, and they need some way to easily add facts to
 * the accumulation they are tracking. This accumulation analysis represents all facts as Strings.
 *
 * <p>Accumulation checkers are particularly interesting because they can represent many interesting
 * typestate-like properties, but do not require a precise alias analysis for soundness.
 *
 * <p>This class provides a basic accumulation checker that can be extended to implement a
 * particular accumulation type system.
 */
public abstract class AccumulationChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ReturnsReceiverChecker.class);
        return checkers;
    }
}
