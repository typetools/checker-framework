package org.checkerframework.checker.regex;


import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.framework.qual.StubFiles;

/**
 * {@link CheckerAdapter} for the Regex-Qual-Param type system.
 */
@StubFiles("apache-xerces.astub")
public class RegexChecker extends CheckerAdapter<QualParams<Regex>> {

    public RegexChecker() {
        super(new RegexQualPolyChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new RegexTypecheckVisitor(this);
    }

    @Override
    public void setupDefaults(QualifierDefaults defaults) {
        defaults.addCheckedCodeDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Regex.BOTTOM))),
                TypeUseLocation.LOWER_BOUND);

        defaults.addCheckedCodeDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Regex.TOP))),
                TypeUseLocation.LOCAL_VARIABLE);
    }

}
