package org.checkerframework.checker.compilermsgs;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.propkey.PropertyKeyAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

/**
 * A PropertyKeyATF that uses CompilerMessageKey to annotate the keys.
 *
 * @author wmdietl
 */
public class CompilerMessagesAnnotatedTypeFactory extends PropertyKeyAnnotatedTypeFactory {

    public CompilerMessagesAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(this);
        implicitsTreeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, PROPKEY_BOTTOM);

        return new ListTreeAnnotator(
                new PropagationTreeAnnotator(this),
                implicitsTreeAnnotator,
                new KeyLookupTreeAnnotator(this, CompilerMessageKey.class)
        );
    }
}
