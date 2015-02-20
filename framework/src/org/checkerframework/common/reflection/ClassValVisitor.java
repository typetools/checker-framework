package org.checkerframework.common.reflection;

import static org.checkerframework.common.reflection.ClassValAnnotatedTypeFactory.getClassNames;

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

import com.sun.source.tree.Tree;

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
class ClassNameValidator extends BaseTypeValidator{

    public ClassNameValidator(BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    @Override
    public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
        AnnotationMirror classVal = type.getAnnotation(ClassVal.class);
        classVal = classVal == null ? type.getAnnotation(ClassBound.class)
                : classVal;
        if (classVal != null) {
            List<String> classNames = getClassNames(classVal);
            for (String className : classNames) {
                if (!isLegalClassName(className)) {
                    checker.report(
                            Result.failure("illegal.classname", className, type), tree);
                }
            }
        }
        return super.isValid(type, tree);
    }

    /**
     * A string is a legal fully qualified class name if it has the following form:
     * ((Java identifier)\.)*(Java identifier)([])*
     * @param className String to check
     * @return true if className is a legal class name
     */
    private boolean isLegalClassName(String className) {
        char[] classNameChars = className.toCharArray();
        ClassNamePart last = ClassNamePart.SEPERATOR;
        for (char c : classNameChars) {
            switch (last) {
            case ID_PART:
            case ID_START:
                switch (c) {
                case '.':
                    last = ClassNamePart.SEPERATOR;
                    break;
                case '[':
                    last = ClassNamePart.OPEN_BRACKET;
                    break;
                default:
                    if (!Character.isJavaIdentifierPart(c)) {
                        return false;
                    }
                    last = ClassNamePart.ID_PART;
                    break;
                }
                break;
            case SEPERATOR:
                if (!Character.isJavaIdentifierStart(c)) {
                    return false;
                }
                last = ClassNamePart.ID_START;
                break;
            case CLOSE_BRACKET:
                if (c == '[') {
                    last = ClassNamePart.OPEN_BRACKET;
                    break;
                }
                return false;
            case OPEN_BRACKET:
                if (c == ']') {
                    last = ClassNamePart.CLOSE_BRACKET;
                    break;
                }
                // only legal char after [ is ]
                return false;
            }
        }
        if (last == ClassNamePart.CLOSE_BRACKET
                || last == ClassNamePart.ID_PART
                || last == ClassNamePart.ID_START) {
            return true;
        }
        return false;
    }

    enum ClassNamePart {
        SEPERATOR, ID_START, ID_PART, OPEN_BRACKET, CLOSE_BRACKET;
    };

}
