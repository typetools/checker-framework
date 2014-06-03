package org.checkerframework.checker.compilermsgs;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.propkey.PropertyKeyAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.ListTreeAnnotator;
import org.checkerframework.framework.type.PropagationTreeAnnotator;
import org.checkerframework.framework.type.TreeAnnotator;

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
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new KeyLookupTreeAnnotator(this, CompilerMessageKey.class)
        );
    }
}
