package org.checkerframework.common.reflection;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.plumelib.reflection.Signatures;

/** A visitor to verify validity of {@code @}{@link ClassVal} annotations. */
public class ClassValVisitor extends BaseTypeVisitor<ClassValAnnotatedTypeFactory> {
    /**
     * Create a new ClassValVisitor.
     *
     * @param checker the associated type-checker
     */
    public ClassValVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected ClassValAnnotatedTypeFactory createTypeFactory() {
        return new ClassValAnnotatedTypeFactory(checker);
    }

    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new ClassNameValidator(checker, this, atypeFactory);
    }
}

class ClassNameValidator extends BaseTypeValidator {

    public ClassNameValidator(
            BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    /**
     * This implementation reports an "illegal.classname" error if the type contains a @ClassVal
     * annotation with a string that is not a valid class name.
     */
    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        AnnotationMirror classVal = type.getAnnotation(ClassVal.class);
        classVal = classVal == null ? type.getAnnotation(ClassBound.class) : classVal;
        if (classVal != null) {
            List<String> classNames =
                    ((ClassValAnnotatedTypeFactory) atypeFactory)
                            .getClassNamesFromAnnotation(classVal);
            for (String className : classNames) {
                if (!Signatures.isFqBinaryName(className)) {
                    checker.reportError(tree, "illegal.classname", className, type);
                }
            }
        }
        return super.visitDeclared(type, tree);
    }
}
