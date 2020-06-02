package org.checkerframework.common.accumulation;

import java.util.LinkedHashSet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;

// TODO: This Javadoc comment should reference the Checker Framework manual, once the Accumulation
// Checker chapter is uncommented in the manual's LaTeX source.
/**
 * An accumulation checker is one that accumulates some property: method calls, map keys, etc.
 *
 * <p>This class provides a basic accumulation analysis that can be extended to implement an
 * accumulation type system. This accumulation analysis represents all facts as Strings. It
 * automatically includes returns-receiver aliasing to precisely handle fluent APIs, but otherwise
 * uses no alias analysis.
 *
 * <p>The primary extension point is the constructor of {@link AccumulationAnnotatedTypeFactory},
 * which every subclass should override to provide custom annotations.
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
