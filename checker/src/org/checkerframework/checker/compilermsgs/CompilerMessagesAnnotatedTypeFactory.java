package org.checkerframework.checker.compilermsgs;

import com.sun.source.tree.Tree;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.propkey.PropertyKeyAnnotatedTypeFactory;
import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.checker.propkey.qual.PropertyKeyBottom;
import org.checkerframework.checker.propkey.qual.UnknownPropertyKey;
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
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll(
                UnknownPropertyKey.class,
                PropertyKey.class, PropertyKeyBottom.class);
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
