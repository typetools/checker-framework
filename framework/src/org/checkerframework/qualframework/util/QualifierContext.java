package org.checkerframework.qualframework.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.base.QualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypes;

public interface QualifierContext<Q> {
    ProcessingEnvironment getProcessingEnvironment();
    Elements getElementUtils();
    Types getTypeUtils();

    Checker<Q> getChecker();
    CheckerAdapter<Q> getCheckerAdapter();
    QualifiedTypeFactory<Q> getTypeFactory();
    QualifiedTypes<Q> getQualifiedTypeUtils();
}
