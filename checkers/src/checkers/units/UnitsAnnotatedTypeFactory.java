package checkers.units;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.units.quals.MixedUnits;
import checkers.units.quals.Prefix;
import checkers.util.AnnotationUtils;
import checkers.util.AnnotationUtils.AnnotationBuilder;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

/**
 * Annotated type factory for the Units Checker.
 * 
 * Handles multiple names for the same unit, with different prefixes,
 * e.g. @kg is the same as @g(Prefix.kilo).
 * 
 * Supports relations between units, e.g. if "m" is a variable of type "@m" and
 * "s" is a variable of type "@s", the division "m/s" is automatically annotated
 * as "mPERs", the correct unit for the result.
 */
public class UnitsAnnotatedTypeFactory extends
        BasicAnnotatedTypeFactory<UnitsChecker> {

    protected final AnnotationMirror mixedUnits;

    public UnitsAnnotatedTypeFactory(UnitsChecker checker,
            CompilationUnitTree root) {
        // use true for flow inference
        super(checker, root, false);

        AnnotationUtils annoUtils = AnnotationUtils.getInstance(env);

        mixedUnits = annoUtils.fromClass(MixedUnits.class);
    }

    private final Map<String, AnnotationMirror> aliasMap = new HashMap<String, AnnotationMirror>();
    
    @Override
    protected AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        String aname = a.getAnnotationType().toString();
        if (aliasMap.containsKey(aname)) {
            return aliasMap.get(aname);
        }
        for (AnnotationMirror aa : a.getAnnotationType().asElement().getAnnotationMirrors() ) {
            // TODO: Is using toString the best way to go?
            if (aa.getAnnotationType().toString().equals("checkers.units.quals.UnitsMultiple")) {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> theclass = (Class<? extends Annotation>)
                                                    AnnotationUtils.parseTypeValue(aa, "quantity");
                Prefix prefix = AnnotationUtils.parseEnumConstantValue(aa, "prefix", Prefix.class);
                AnnotationBuilder builder = new AnnotationBuilder(env, theclass);
                builder.setValue("value", prefix);
                AnnotationMirror res = builder.build();
                aliasMap.put(aname, res);
                return res;
            }
        }
        return super.aliasedAnnotation(a);
    }
    
    @Override
    protected TreeAnnotator createTreeAnnotator(UnitsChecker checker) {
        return new UnitsTreeAnnotator(checker);
    }

    /**
     * A class for adding annotations based on tree
     */
    private class UnitsTreeAnnotator extends TreeAnnotator {

        UnitsTreeAnnotator(BaseTypeChecker checker) {
            super(checker, UnitsAnnotatedTypeFactory.this);
        }
        
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror lht = getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror rht = getAnnotatedType(node.getRightOperand());
            Kind kind = node.getKind();
            
            AnnotationMirror bestres = null;
            for (UnitsRelations ur : checker.unitsRel.values()) {
                AnnotationMirror res = useUnitsRelation(kind, ur, lht, rht); 
                
                if (bestres != null && res != null && !bestres.equals(res)) {
                    // TODO: warning
                    System.out.println("UnitsRelation mismatch, taking neither! Previous: "
                                    + bestres + " and current: " + res);
                    return super.visitBinary(node, type);
                }

                if (res!=null) {
                    bestres = res;
                }
            }

            if (bestres!=null) {
                type.addAnnotation(bestres);
            } else {
                // Did not find a UnitsRelation, only propagate through scalars.
                
                switch(kind) {
                case DIVIDE:
                    if (lht.getAnnotations().equals(rht.getAnnotations())) {
                        // If the units of the division match,
                        // do not add an annotation to the result type, keep it
                        // unqualified.
                        break;
                    }
                case MULTIPLY:
                    if (lht.getAnnotations().isEmpty()) {
                        type.addAnnotations(rht.getAnnotations());
                        break;
                    }
                    if (rht.getAnnotations().isEmpty()) {
                        type.addAnnotations(lht.getAnnotations());
                        break;
                    }
                    type.addAnnotation(mixedUnits);
                    break;
                }
            }

            return super.visitBinary(node, type);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            ExpressionTree var = node.getVariable();
            AnnotatedTypeMirror varType = getAnnotatedType(var);

            type.clearAnnotations();
            type.addAnnotations(varType.getAnnotations());
            return super.visitCompoundAssignment(node, type);
        }
        
        private AnnotationMirror useUnitsRelation(Kind kind, UnitsRelations ur,
                AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {
            
            AnnotationMirror res = null;
            if (ur!=null) {
                switch(kind) {
                case DIVIDE:
                    res = ur.division(lht, rht);
                    break;
                case MULTIPLY:
                    res = ur.multiplication(lht, rht);
                    break;
                }
            }
            return res;
        }

    }

}
