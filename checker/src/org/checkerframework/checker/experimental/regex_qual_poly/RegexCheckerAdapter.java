package org.checkerframework.checker.experimental.regex_qual_poly;


import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;

/**
 * {@link CheckerAdapter} for the Regex-Qual-Param type system.
 */
public class RegexCheckerAdapter extends CheckerAdapter<QualParams<Regex>> {

    public RegexCheckerAdapter() {
        super(new RegexQualPolyChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new RegexTypecheckVisitor(this);
    }

    @Override
    public void setupDefaults(QualifierDefaults defaults) {
        defaults.addAbsoluteDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Regex.BOTTOM))),
                DefaultLocation.LOWER_BOUNDS);

        defaults.addAbsoluteDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Regex.TOP))),
                DefaultLocation.LOCAL_VARIABLE);
    }

}
