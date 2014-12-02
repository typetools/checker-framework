package org.checkerframework.checker.units;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * Units Checker main class.
 * <p>
 *
 * Supports "units" option to add support for additional units.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@SupportedOptions( { "units" } )
public class UnitsChecker extends BaseTypeChecker {

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */


    /** Copied from SubtypingChecker; cannot reuse it, because SubtypingChecker is final.
     * TODO: SubtypingChecker might also want to always call super.
     */
    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Set<String> swKeys = new HashSet<String>(super.getSuppressWarningsKeys());
        Set<Class<? extends Annotation>> annos = ((BaseTypeVisitor<?>)visitor).getTypeFactory().getSupportedTypeQualifiers();

        for (Class<? extends Annotation> anno : annos) {
            swKeys.add(anno.getSimpleName().toLowerCase());
        }

        return swKeys;
    }
}
