package org.checkerframework.qualframework.base;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationFormatter;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.format.QualifiedTypeFormatter;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collection;

/** Adapter class for {@link Checker}, extending
 * {@link BaseTypeChecker org.checkerframework.common.basetype.BaseTypeChecker}.
 */
public class CheckerAdapter<Q> extends BaseTypeChecker {
    /** The underlying qualifier-based checker. */
    private final Checker<Q> underlying;
    /** The {@link TypeMirrorConverter} used by this {@link CheckerAdapter} and
     * its components. */
    private TypeMirrorConverter<Q> typeMirrorConverter;
    /** The adapter for the underlying checker's {@link QualifiedTypeFactory}.
     */
    private QualifiedTypeFactoryAdapter<Q> typeFactory;

    /** Constructs a {@link CheckerAdapter} from an underlying qualifier-based
     * {@link Checker}. */
    public CheckerAdapter(Checker<Q> underlying) {
        this.underlying = underlying;
        underlying.setAdapter(this);
    }

    /** Gets the {@link TypeMirrorConverter} used by this {@link CheckerAdapter}
     * and its component adapters. */
    public TypeMirrorConverter<Q> getTypeMirrorConverter() {
        if (this.typeMirrorConverter == null) {
            this.typeMirrorConverter =
                new TypeMirrorConverter<Q>(getProcessingEnvironment(), this);
        }
        return this.typeMirrorConverter;
    }

    /**
     * Gets the {@link QualifiedTypeFactoryAdapter} for the underlying
     * checker's {@link QualifiedTypeFactory}.  This is used by the {@link
     * org.checkerframework.framework.source.SourceVisitor} defined below to
     * obtain the {@link QualifiedTypeFactoryAdapter} using lazy
     * initialization.
     */
    @Override
    public QualifiedTypeFactoryAdapter<Q> getTypeFactory() {
        // TODO: check if lazy init is actually necessary for typeFactory.
        if (typeFactory == null) {
            typeFactory = createTypeFactory();
            // We have to delay postInit until after the typeFactory field has
            // been set.
            //
            // ATF.postInit runs some initialization steps that require the
            // TypeMirrorConverter to be ready.  The TMC requires an ATF
            // instance, so it calls this getTypeFactory method.  That leads to
            // infinite recurison through postInit -> some TMC method ->
            // getTypeFactory -> createTypeFactory -> postInit.  To avoid this,
            // we delay postInit until after typeFactory has been initialized,
            // to break the getTypeFactory -> createTypeFactory edge of the
            // cycle.
            typeFactory.doPostInit();
        }
        return typeFactory;
    }

    /** Constructs a {@link QualifiedTypeFactoryAdapter} for the underlying
     * {@link QualifiedTypeFactory}. */
    private QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
        QualifiedTypeFactory<Q> underlyingFactory = underlying.getTypeFactory();

        final QualAnnotationFormatterAdapter formatterAdapter =
                new QualAnnotationFormatterAdapter(underlying.createQualifiedTypeFormatter());

        QualifiedTypeFactoryAdapter<Q> factoryAdapter = new QualifiedTypeFactoryAdapter<Q>(
                underlyingFactory,
                this) {

            @Override
            protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
                return formatterAdapter;
            }

            @Override
            protected AnnotationFormatter createAnnotationFormatter() {
                return formatterAdapter;
            }
        };

        if (underlyingFactory instanceof DefaultQualifiedTypeFactory) {
            DefaultQualifiedTypeFactory<Q> defaultFactory =
                (DefaultQualifiedTypeFactory<Q>)underlyingFactory;
            defaultFactory.setAdapter(factoryAdapter);
        }

        return factoryAdapter;
    }

    /**
     * Adapter to convert an AnnotatedTypeFormatter to a QualifiedTypeFormatter.
     */
    protected class QualAnnotationFormatterAdapter implements AnnotatedTypeFormatter, AnnotationFormatter {

        private final QualifiedTypeFormatter<Q> qualifiedTypeFormatter;

        public QualAnnotationFormatterAdapter(QualifiedTypeFormatter<Q> qualifiedTypeFormatter) {
            this.qualifiedTypeFormatter = qualifiedTypeFormatter;
        }

        @Override
        public String format(AnnotatedTypeMirror type) {
            return qualifiedTypeFormatter.format(getTypeMirrorConverter().getQualifiedType(type));
        }

        @Override
        public String format(AnnotatedTypeMirror type, boolean printInvisibles) {
            return qualifiedTypeFormatter.format(getTypeMirrorConverter().getQualifiedType(type), printInvisibles);
        }

        @Override
        public String formatAnnotationString(Collection<? extends AnnotationMirror> annos, boolean printInvisible) {
            StringBuilder sb = new StringBuilder();
            for (AnnotationMirror anno : annos) {
                String result = qualifiedTypeFormatter.getQualFormatter().format(
                        getTypeMirrorConverter().getQualifier(anno), printInvisible);

                if (result != null) {
                    sb.append(result);
                    sb.append(" ");
                }
            }
            return sb.toString();
        }

        @Override
        public String formatAnnotationMirror(AnnotationMirror anno) {
            return qualifiedTypeFormatter.getQualFormatter().format(getTypeMirrorConverter().getQualifier(anno));

        }
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new BaseTypeVisitor<QualifiedTypeFactoryAdapter<Q>>(this) {
            @Override
            protected QualifiedTypeFactoryAdapter<Q> createTypeFactory() {
                return CheckerAdapter.this.getTypeFactory();
            }
        };
    }

    public Checker<Q> getUnderlying() {
        return underlying;
    }

    public void setupDefaults(QualifierDefaults defaults) {
        defaults.addCheckedCodeDefault(
                getTypeMirrorConverter().getAnnotation(
                        underlying.getTypeFactory().getQualifierHierarchy().getBottom()),
                TypeUseLocation.IMPLICIT_LOWER_BOUND);
        defaults.addCheckedCodeDefault(
                getTypeMirrorConverter().getAnnotation(
                        underlying.getTypeFactory().getQualifierHierarchy().getTop()),
                TypeUseLocation.LOCAL_VARIABLE);
    }
}
