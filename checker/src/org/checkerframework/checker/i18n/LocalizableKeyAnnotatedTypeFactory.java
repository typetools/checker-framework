package org.checkerframework.checker.i18n;

import org.checkerframework.checker.i18n.qual.LocalizableKey;
import org.checkerframework.checker.propkey.PropertyKeyAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.TreeAnnotator;

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
    public TreeAnnotator createTreeAnnotator() {
        return new KeyLookupTreeAnnotator(this, LocalizableKey.class);
    }
}
