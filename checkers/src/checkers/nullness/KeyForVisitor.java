package checkers.nullness;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeValidator;
import checkers.basetype.BaseTypeVisitor;
import checkers.nullness.quals.KeyFor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

import javacutils.AnnotationUtils;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;

public class KeyForVisitor extends BaseTypeVisitor<KeyForSubchecker, KeyForAnnotatedTypeFactory> {
    public KeyForVisitor(KeyForSubchecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    /**
     * The type validator to ensure correct usage of ownership modifiers.
     */
    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new KeyForTypeValidator(checker, this, atypeFactory);
    }

    private final static class KeyForTypeValidator extends BaseTypeValidator {

        public KeyForTypeValidator(BaseTypeChecker<?> checker,
                BaseTypeVisitor<?, ?> visitor, AnnotatedTypeFactory atypeFactory) {
            super(checker, visitor, atypeFactory);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Tree p) {
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

            return super.visitDeclared(type, p);
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
}
