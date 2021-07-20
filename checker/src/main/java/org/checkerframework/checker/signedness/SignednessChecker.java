package org.checkerframework.checker.signedness;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

import java.util.LinkedHashSet;

/**
 * A type-checker that prevents mixing of unsigned and signed values, and prevents meaningless
 * operations on unsigned values.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@RelevantJavaTypes({
    Byte.class,
    Short.class,
    Integer.class,
    Long.class,
    Character.class,
    byte.class,
    short.class,
    int.class,
    long.class,
    char.class
})
public class SignednessChecker extends BaseTypeChecker {

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
