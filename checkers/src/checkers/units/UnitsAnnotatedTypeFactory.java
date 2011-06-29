package checkers.units;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.units.quals.Prefix;
import checkers.util.AnnotationUtils;
import checkers.util.AnnotationUtils.AnnotationBuilder;

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

    public UnitsAnnotatedTypeFactory(UnitsChecker checker,
            CompilationUnitTree root) {
        // use true for flow inference
        super(checker, root, false);
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
            
            switch(kind) {
            case DIVIDE:
            case MULTIPLY:
                if (lht.getAnnotations().isEmpty()) {
                    type.addAnnotations(rht.getAnnotations());
                    return super.visitBinary(node, type);
                }
                if (rht.getAnnotations().isEmpty()) {
                    type.addAnnotations(lht.getAnnotations());
                    return super.visitBinary(node, type);
                }
                break;
            }
            
            UnitsRelations lhur = findUnitsRelations(lht);
            UnitsRelations rhur = findUnitsRelations(rht);
            
            AnnotationMirror lhres = useUnitsRelation(kind, lhur, lht, rht); 
            AnnotationMirror rhres = useUnitsRelation(kind, rhur, lht, rht); 

            if (lhres!=null && rhres!=null && !lhres.equals(rhres)) {
                // TODO: warning
                System.out.println("UnitsRelation mismatch, taking neither! LType: " + lhres + " and RType: " + rhres);
                return super.visitBinary(node, type);
            }

            if (lhres!=null) {
                type.addAnnotation(lhres);
            } else if (rhres!=null) {
                type.addAnnotation(rhres);
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

    private UnitsRelations findUnitsRelations(AnnotatedTypeMirror type) {
        // TODO: look at the units annotation and see whether it
        // has a @UnitsRelations meta-annotation.
        // Then do the following reflectively with the given class or
        // this as default result.
        return new UnitsRelationsDefault().init(annotations, env);
    }

}
