package org.checkerframework.checker.i18n;

import com.sun.source.tree.Tree;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.checkerframework.checker.i18n.qual.LocalizableKey;
import org.checkerframework.checker.propkey.PropertyKeyAnnotatedTypeFactory;
import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.checker.propkey.qual.UnknownPropertyKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

/**
 * A PropertyKeyATF that uses LocalizableKey to annotate the keys.
 *
 * @author wmdietl
 */
public class LocalizableKeyAnnotatedTypeFactory extends
        PropertyKeyAnnotatedTypeFactory {

    public LocalizableKeyAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<Class<? extends Annotation>>(
                        Arrays.asList(LocalizableKey.class, PropertyKey.class, UnknownPropertyKey.class)));
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(this);
        implicitsTreeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, PROPKEY_BOTTOM);

        return new ListTreeAnnotator(
                new PropagationTreeAnnotator(this),
                implicitsTreeAnnotator,
                new KeyLookupTreeAnnotator(this, LocalizableKey.class));
    }
}
