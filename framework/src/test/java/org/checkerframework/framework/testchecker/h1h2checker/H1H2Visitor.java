package org.checkerframework.framework.testchecker.h1h2checker;

import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;

public class H1H2Visitor extends BaseTypeVisitor<H1H2AnnotatedTypeFactory> {

    public H1H2Visitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new H1H2TypeValidator(checker, this, atypeFactory);
    }

    private final class H1H2TypeValidator extends BaseTypeValidator {

        public H1H2TypeValidator(
                BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor,
                AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
            AnnotationMirror h1Invalid = AnnotationBuilder.fromClass(elements, H1Invalid.class);
            if (AnnotatedTypes.containsModifier(type, h1Invalid)) {
                checker.reportError(
                        p,
                        // An error specific to this type system, with no corresponding text
                        // in a messages.properties file; this checker is just for testing.
                        "h1h2checker.h1invalid.forbidden",
                        type.getAnnotations(),
                        type.toString());
            }
            return super.visitDeclared(type, p);
        }
    }
}
