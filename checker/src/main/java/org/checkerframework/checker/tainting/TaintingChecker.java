package org.checkerframework.checker.tainting;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SuppressWarningsKeys;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.SimpleHierarchy;

/**
 * A type-checker plug-in for the Tainting type system qualifier that finds (and verifies the
 * absence of) trust bugs.
 *
 * <p>It verifies that only verified values are trusted and that user-input is sanitized before use.
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@SuppressWarningsKeys({"untainted", "tainting"})
public class TaintingChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new BaseTypeVisitor<BaseAnnotatedTypeFactory>(this) {
            @Override
            protected BaseAnnotatedTypeFactory createTypeFactory() {
                return new TaintingAnnotatedTypeFactory(TaintingChecker.this);
            }
        };
    }

    static class TaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
        TaintingAnnotatedTypeFactory(TaintingChecker checker) {
            super(checker);
            postInit();
        }

        @Override
        public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
            return new SimpleHierarchy(getSupportedTypeQualifiers(), elements);
        }
    }
}
