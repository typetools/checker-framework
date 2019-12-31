package org.checkerframework.checker.compilermsgs;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.compilermsgs.qual.UnknownCompilerMessageKey;
import org.checkerframework.checker.propkey.PropertyKeyAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

/** A PropertyKeyATF that uses CompilerMessageKey to annotate the keys. */
public class CompilerMessagesAnnotatedTypeFactory extends PropertyKeyAnnotatedTypeFactory {

    public CompilerMessagesAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        // Does not call postInit() because its superclass does.
        // If we ever add code to this constructor, it needs to:
        //   * call a superclass constructor that does not call postInit(), and
        //   * call postInit() itself.
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(CompilerMessageKey.class, UnknownCompilerMessageKey.class));
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createBasicTreeAnnotator(),
                new KeyLookupTreeAnnotator(this, CompilerMessageKey.class));
    }
}
