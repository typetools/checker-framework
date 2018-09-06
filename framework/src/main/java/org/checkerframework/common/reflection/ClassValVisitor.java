package org.checkerframework.common.reflection;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class ClassValVisitor extends BaseTypeVisitor<ClassValAnnotatedTypeFactory> {
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
     * Reports an "illegal.classname" error if the type contains a classVal annotation with
     * classNames that cannot possibly be valid class annotations.
     */
    @Override
    public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
        AnnotationMirror classVal = type.getAnnotation(ClassVal.class);
        classVal = classVal == null ? type.getAnnotation(ClassBound.class) : classVal;
        if (classVal != null) {
            List<String> classNames =
                    ClassValAnnotatedTypeFactory.getClassNamesFromAnnotation(classVal);
            for (String className : classNames) {
                if (!isLegalClassName(className)) {
                    checker.report(Result.failure("illegal.classname", className, type), tree);
                }
            }
        }
        return super.isValid(type, tree);
    }

    /**
     * A string is a legal binary name if it has the following form: ((Java identifier)\.)*(Java
     * identifier)([])* https://docs.oracle.com/javase/specs/jls/se10/html/jls-13.html#jls-13.1
     *
     * @param className string to check
     * @return true if className is a legal class name
     */
    private boolean isLegalClassName(String className) {
        int lastBracket = className.lastIndexOf("]");
        if (lastBracket != -1 && lastBracket != className.length() - 1) {
            return false;
        }
        className = className.replaceAll("\\[\\]", "");
        String[] identifiers = className.split("(\\.)");
        for (String identifier : identifiers) {
            if (!isJavaIdentifier(identifier)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the given string is a Java Identifier. (This method returns true if the Identifier is
     * a keyword, boolean literal, null literal.
     */
    private boolean isJavaIdentifier(String identifier) {
        char[] identifierChars = identifier.toCharArray();
        if (!(identifierChars.length > 0
                && (Character.isJavaIdentifierStart(identifierChars[0])))) {
            return false;
        }
        for (int i = 1; i < identifierChars.length; i++) {
            if (!Character.isJavaIdentifierPart(identifierChars[i])) {
                return false;
            }
        }
        return true;
    }
}
