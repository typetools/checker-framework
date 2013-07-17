package checkers.oigj;

import checkers.oigj.quals.World;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;

import javacutils.TypesUtils;

import javax.lang.model.element.ElementKind;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;

public class OwnershipAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<OwnershipSubchecker> {

    public OwnershipAnnotatedTypeFactory(OwnershipSubchecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.postInit();
    }


    @Override
    protected TreeAnnotator createTreeAnnotator(OwnershipSubchecker checker) {
        return new OwnershipTreeAnnotator(checker);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator(OwnershipSubchecker checker) {
        return new OwnershipTypeAnnotator(checker);
    }

    private class OwnershipTypeAnnotator extends TypeAnnotator {

        public OwnershipTypeAnnotator(OwnershipSubchecker checker) {
            super(checker, OwnershipAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, ElementKind p) {
            if (type.isAnnotatedInHierarchy(checker.BOTTOM_QUAL))
                return super.visitDeclared(type, p);

            if (p == ElementKind.CLASS && TypesUtils.isObject(type.getUnderlyingType()))
                type.addAnnotation(World.class);
            return super.visitDeclared(type, p);
        }
    }

    private class OwnershipTreeAnnotator extends TreeAnnotator {
        public OwnershipTreeAnnotator(OwnershipSubchecker checker) {
            super(checker, OwnershipAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(checker.BOTTOM_QUAL);
            return super.visitBinary(node, type);
        }
    }
}
