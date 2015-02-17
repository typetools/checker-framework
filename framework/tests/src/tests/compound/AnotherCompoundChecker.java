package tests.compound;

import java.util.LinkedHashSet;

import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

import tests.compound.qual.ACCBottom;
import tests.compound.qual.ACCTop;

@TypeQualifiers({ ACCTop.class, ACCBottom.class })
public class AnotherCompoundChecker extends BaseTypeChecker {
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> subcheckers = new LinkedHashSet<>();
        subcheckers.add(AliasingChecker.class);
        subcheckers.add(ValueChecker.class);
        return subcheckers;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new BaseTypeVisitor<AnotherCompoundCheckerAnnotatedTypeFactory>(
                this) {
            @Override
            protected AnotherCompoundCheckerAnnotatedTypeFactory createTypeFactory() {
                return new AnotherCompoundCheckerAnnotatedTypeFactory(checker);
            }
        };
    }
}
