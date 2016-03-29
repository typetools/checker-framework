package org.checkerframework.checker.nullness;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.NewClassTree;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;

public class KeyForVisitor extends BaseTypeVisitor<KeyForAnnotatedTypeFactory> {
    public KeyForVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ String errorKey) {

        atypeFactory.keyForCanonicalizeValues(varType, valueType, getCurrentPath());

        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }

    /**
     * The type validator to ensure correct usage of the 'static' modifier.
     */
    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new KeyForTypeValidator(checker, this, atypeFactory);
    }

    private final static class KeyForTypeValidator extends BaseTypeValidator {

        public KeyForTypeValidator(BaseTypeChecker checker,
                BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
            // Verify that a static variable cannot be @KeyFor("this")

            AnnotationMirror kf = type.getAnnotation(KeyFor.class);
            if (kf != null) {
                List<String> maps = AnnotationUtils.getElementValueArray(kf, "value", String.class, false);

                boolean inStatic = false;
                if (p.getKind() == Kind.VARIABLE) {
                    ModifiersTree mt = ((VariableTree) p).getModifiers();
                    if (mt.getFlags().contains(Modifier.STATIC)) {
                        inStatic = true;
                    }
                }

                for (String map : maps) {
                    if (map.equals("this")) {
                        // this is not valid in static context
                        if (inStatic) {
                            checker.report(
                                    Result.failure("keyfor.type.invalid",
                                            type.getAnnotations(),
                                            type.toString()), p);
                        }
                    } else if (map.matches("#(\\d+)")) {
                        // Accept parameter references
                        // TODO: look for total number of parameters and only
                        // allow the range 0 to n-1
                    } else {
                        // Only other option is local variable and field names?
                        // TODO: go through all possibilities.
                    }
                }
            }

            // TODO: Should BaseTypeValidator be parametric in the ATF?
            if (type.isAnnotatedInHierarchy(((KeyForAnnotatedTypeFactory)atypeFactory).KEYFOR)) {
                return super.visitDeclared(type, p);
            } else {
                // TODO: Something went wrong...
                return null;
            }
        }

        // TODO: primitive types? arrays?
        /*
        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Tree p) {
          return super.visitPrimitive(type, p);
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Tree p) {
          return super.visitArray(type, p);
        }
        */
    }


    /**
     * The constructor type will have its flow expressions parsed and its KeyFor values
     * canonicalized before this point (in constructorFromUse).  However, the expectedReturnType
     * will not.  Canonicalize it now.
     */
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType expectedReturnType,
                                                 AnnotatedExecutableType constructor, Tree src) {

        NewClassTree invocation = (NewClassTree) src;
        atypeFactory.canonicalizeForViewpointAdaptation(invocation, expectedReturnType);
        return super.checkConstructorInvocation(expectedReturnType, constructor, invocation);
    }
}
