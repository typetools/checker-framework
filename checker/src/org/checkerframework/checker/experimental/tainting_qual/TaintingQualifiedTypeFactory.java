package org.checkerframework.checker.experimental.tainting_qual;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.TreeAnnotator;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;

public class TaintingQualifiedTypeFactory extends DefaultQualifiedTypeFactory<Tainting> {

    public TaintingQualifiedTypeFactory(QualifierContext<Tainting> context) {
        super(context);
    }

    @Override
    protected QualifierHierarchy<Tainting> createQualifierHierarchy() {
        return new TaintingQualifierHierarchy();
    }

    @Override
    protected TaintingAnnotationConverter createAnnotationConverter() {
        return new TaintingAnnotationConverter();
    }

    @Override
    protected TreeAnnotator<Tainting> createTreeAnnotator() {
        return new TreeAnnotator<Tainting>() {
            @Override
            public QualifiedTypeMirror<Tainting> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                QualifiedTypeMirror<Tainting> result = super.visitLiteral(tree, type);
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    result = SetQualifierVisitor.apply(result, Tainting.UNTAINTED);
                }
                return result;
            }
        };
    }
}
