package org.checkerframework.qualframework.util;

import org.checkerframework.framework.util.BaseContext;
import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.base.QualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypes;

public interface QualifierContext<Q> extends BaseContext {
    Checker<Q> getChecker();
    CheckerAdapter<Q> getCheckerAdapter();
    QualifiedTypeFactory<Q> getTypeFactory();
    QualifiedTypes<Q> getQualifiedTypeUtils();
}
