package org.checkerframework.framework.base;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import checkers.basetype.BaseAnnotatedTypeFactory;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.types.visitors.SimpleAnnotatedTypeVisitor;
import checkers.util.AnnotationBuilder;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import org.checkerframework.framework.base.QualifiedTypeMirror.*;
import org.checkerframework.framework.util.WrappedAnnotatedTypeMirror;

class TypeMirrorConverter<Q> {
    private CheckerAdapter<Q> checkerAdapter;
    private ProcessingEnvironment processingEnv;
    private ExecutableElement indexElement;
    private AnnotationMirror blankKey;
    private QualifiedTypeFactoryAdapter<Q> typeFactory;

    private int nextIndex = 0;

    private HashMap<Q, Integer> qualToIndex;
    private HashMap<Integer, Q> indexToQual;

    @TypeQualifier
    @SubtypeOf({})
    public static @interface Key {
        int index() default -1;
        String desc() default "";
    }


    public TypeMirrorConverter(ProcessingEnvironment processingEnv, CheckerAdapter<Q> checkerAdapter) {
        this.checkerAdapter = checkerAdapter;
        this.processingEnv = processingEnv;
        this.indexElement = TreeUtils.getMethod(Key.class.getCanonicalName(), "index", 0, processingEnv);
        this.blankKey = AnnotationUtils.fromClass(processingEnv.getElementUtils(), Key.class);
        // typeFactory will be lazily initialized, to break a circular
        // dependency between this class and QualifiedTypeFactoryAdapter.
        this.typeFactory = null;

        this.qualToIndex = new HashMap<>();
        this.indexToQual = new HashMap<>();
    }

    private QualifiedTypeFactoryAdapter<Q> getTypeFactory() {
        if (typeFactory == null) {
            typeFactory = checkerAdapter.getTypeFactory();
        }
        return typeFactory;
    }

    private AnnotationMirror createKey(int index, Object desc) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Key.class.getCanonicalName());
        builder.setValue("index", index);
        builder.setValue("desc", "" + desc);
        return builder.build();
    }


    private int getIndexForQualifier(Q qual) {
        if (qualToIndex.containsKey(qual)) {
            return qualToIndex.get(qual);
        } else {
            int index = nextIndex++;
            qualToIndex.put(qual, index);
            indexToQual.put(index, qual);
            return index;
        }
    }

    private int getIndex(AnnotationMirror anno) {
        return getAnnotationField(anno, indexElement);
    }


    private int getAnnotationField(AnnotationMirror anno, ExecutableElement element) {
        AnnotationValue value = anno.getElementValues().get(element);
        if (value == null) {
            throw new IllegalArgumentException("@Key annotation contains no " + element);
        }
        assert(value.getValue() instanceof Integer);
        Integer index = (Integer)value.getValue();
        return index;
    }


    /* QTM -> ATM conversion functions */

    /** Given a QualifiedTypeMirror and an AnnotatedTypeMirror with the same
     * underlying TypeMirror, recursively update annotations on the
     * AnnotatedTypeMirror to match the qualifiers present on the
     * QualifiedTypeMirror.  After running applyQualifiers, calling
     * getQualifiedType on the AnnotatedTypeMirror should produce a
     * QualifiedTypeMirror which is identical to the one initially passed to
     * applyQualifiers.
     */
    public void applyQualifiers(QualifiedTypeMirror<Q> qtm, AnnotatedTypeMirror atm) {
        if (qtm == null && atm == null) {
            return;
        }
        assert qtm != null && atm != null;

        // Apply the qualifier for this QTM-ATM pair.
        int index = getIndexForQualifier(qtm.getQualifier());
        AnnotationMirror key = createKey(index, qtm.getQualifier());

        atm.clearAnnotations();
        atm.addAnnotation(key);

        // Recursively create entries for all component QTM-ATM pairs.
        APPLY_COMPONENT_QUALIFIERS_VISITOR.visit(qtm, atm);
    }

    /** Given a QualifiedTypeMirror, produce an AnnotatedTypeMirror with the
     * same underlying TypeMirror and with annotations corresponding to the
     * qualifiers on the input QualifiedTypeMirror.  As with applyQualifiers,
     * calling getQualifiedType on the resulting AnnotatedTypeMirror should
     * produce a QualifiedTypeMirror that is identical to the one passed as
     * input.
     */
    public AnnotatedTypeMirror getAnnotatedType(QualifiedTypeMirror<Q> qtm) {
        if (qtm == null) {
            return null;
        }
        AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(
                qtm.getUnderlyingType().getRaw(), getTypeFactory());
        applyQualifiers(qtm, atm);
        return atm;
    }

    /** applyQualifiers lifted to operate on lists of augmented TypeMirrors.
     */
    private void applyQualifiersToLists(List<? extends QualifiedTypeMirror<Q>> qtms, List<? extends AnnotatedTypeMirror> atms) {
        assert(qtms.size() == atms.size());
        for (int i = 0; i < qtms.size(); ++i) {
            applyQualifiers(qtms.get(i), atms.get(i));
        }
    }

    /** A visitor to recursively applyQualifiers to components of the
     * TypeMirrors.
     *
     * This also has some special handling for ExecutableTypes.  Both
     * AnnotatedTypeMirror and ExtendedTypeMirror (the underlying types used by
     * QualifiedTypeMirror) track the corresponding ExecutableElement (unlike
     * raw javac TypeMirrors), and this visitor updates that element if
     * necessary to make the augmented TypeMirrors correspond.
     */
    private SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror> APPLY_COMPONENT_QUALIFIERS_VISITOR =
        new SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror>() {
            public Void visitArray(QualifiedArrayType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedArrayType atm = (AnnotatedArrayType)rawAtm;
                applyQualifiers(qtm.getComponentType(), atm.getComponentType());
                return null;
            }

            public Void visitDeclared(QualifiedDeclaredType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedDeclaredType atm = (AnnotatedDeclaredType)rawAtm;
                applyQualifiersToLists(qtm.getTypeArguments(), atm.getTypeArguments());  
                return null;
            }

            public Void visitExecutable(QualifiedExecutableType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedExecutableType atm = (AnnotatedExecutableType)rawAtm;

                // Update the ExecutableElement if necessary.  This should
                // happen before the recursive applyQualifiers calls, since it
                // may cause the receiver and return types of the ATM to become
                // non-null.
                ExecutableElement elt = qtm.getUnderlyingType().asElement();
                if (atm.getElement() != elt) {
                    assert elt != null;
                    atm.setElement(elt);
                }

                applyQualifiersToLists(qtm.getParameterTypes(), atm.getParameterTypes());
                applyQualifiers(qtm.getReceiverType(), atm.getReceiverType());
                applyQualifiers(qtm.getReturnType(), atm.getReturnType());
                applyQualifiersToLists(qtm.getThrownTypes(), atm.getThrownTypes());
                applyQualifiersToLists(qtm.getTypeVariables(), atm.getTypeVariables());
                return null;
            }

            public Void visitIntersection(QualifiedIntersectionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedIntersectionType atm = (AnnotatedIntersectionType)rawAtm;
                applyQualifiersToLists(qtm.getBounds(), atm.directSuperTypes());
                return null;
            }

            public Void visitNoType(QualifiedNoType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // NoType has no components.
                return null;
            }

            public Void visitNull(QualifiedNullType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // NullType has no components.
                return null;
            }

            public Void visitPrimitive(QualifiedPrimitiveType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // PrimitiveType has no components.
                return null;
            }

            public Void visitTypeVariable(QualifiedTypeVariable<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedTypeVariable atm = (AnnotatedTypeVariable)rawAtm;
                applyQualifiers(qtm.getLowerBound(), atm.getLowerBound());
                applyQualifiers(qtm.getUpperBound(), atm.getUpperBound());
                return null;
            }

            public Void visitUnion(QualifiedUnionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedUnionType atm = (AnnotatedUnionType)rawAtm;
                applyQualifiersToLists(qtm.getAlternatives(), atm.getAlternatives());
                return null;
            }

            public Void visitWildcard(QualifiedWildcardType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedWildcardType atm = (AnnotatedWildcardType)rawAtm;
                applyQualifiers(qtm.getExtendsBound(), atm.getExtendsBound());
                applyQualifiers(qtm.getSuperBound(), atm.getSuperBound());
                return null;
            }
        };


    /* ATM -> QTM conversion functions */

    /** Given an AnnotatedTypeMirror, construct a QualifiedTypeMirror with the
     * same underlying type with the qualifiers extracted from the
     * AnnotatedTypeMirror's @Key annotations.
     */
    public QualifiedTypeMirror<Q> getQualifiedType(AnnotatedTypeMirror atm) {
        if (atm == null) {
            return null;
        }
        return UPDATED_QTM_BUILDER.visit(atm, getQualifier(atm));
    }

    /** getQualifiedType lifted to operate on a list of AnnotatedTypeMirrors.
     */
    List<QualifiedTypeMirror<Q>> getQualifiedTypeList(List<? extends AnnotatedTypeMirror> atms) {
        List<QualifiedTypeMirror<Q>> result = new ArrayList<>();
        for (int i = 0; i < atms.size(); ++i) {
            result.add(getQualifiedType(atms.get(i)));
        }
        return result;
    }

    /** A visitor to recursively construct QualifiedTypeMirrors from
     * AnnotatedTypeMirrors.
     */
    private SimpleAnnotatedTypeVisitor<QualifiedTypeMirror<Q>, Q> UPDATED_QTM_BUILDER =
        new SimpleAnnotatedTypeVisitor<QualifiedTypeMirror<Q>, Q>() {
            public QualifiedTypeMirror<Q> visitArray(AnnotatedArrayType atm, Q qual) {
                return new QualifiedArrayType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedType(atm.getComponentType()));
            }

            public QualifiedTypeMirror<Q> visitDeclared(AnnotatedDeclaredType atm, Q qual) {
                return new QualifiedDeclaredType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedTypeList(atm.getTypeArguments()));
            }

            public QualifiedTypeMirror<Q> visitExecutable(AnnotatedExecutableType atm, Q qual) {
                List<AnnotatedTypeVariable> annotatedTypeVariables = atm.getTypeVariables();
                List<QualifiedTypeVariable<Q>> qualifiedTypeVariables = new ArrayList<>();
                for (int i = 0; i < annotatedTypeVariables.size(); ++i) {
                    @SuppressWarnings("unchecked")
                    QualifiedTypeVariable<Q> qualified =
                        (QualifiedTypeVariable<Q>)getQualifiedType(annotatedTypeVariables.get(i));
                    qualifiedTypeVariables.add(qualified);
                }

                return new QualifiedExecutableType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedTypeList(atm.getParameterTypes()),
                        getQualifiedType(atm.getReceiverType()),
                        getQualifiedType(atm.getReturnType()),
                        getQualifiedTypeList(atm.getThrownTypes()),
                        qualifiedTypeVariables);
            }

            public QualifiedTypeMirror<Q> visitIntersection(AnnotatedIntersectionType atm, Q qual) {
                return new QualifiedIntersectionType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedTypeList(atm.directSuperTypes()));
            }

            public QualifiedTypeMirror<Q> visitNoType(AnnotatedNoType atm, Q qual) {
                // NoType has no components.
                return new QualifiedNoType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual);
            }

            public QualifiedTypeMirror<Q> visitNull(AnnotatedNullType atm, Q qual) {
                // NullType has no components.
                return new QualifiedNullType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual);
            }

            public QualifiedTypeMirror<Q> visitPrimitive(AnnotatedPrimitiveType atm, Q qual) {
                // PrimitiveType has no components.
                return new QualifiedPrimitiveType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual);
            }

            public QualifiedTypeMirror<Q> visitTypeVariable(AnnotatedTypeVariable atm, Q qual) {
                return new QualifiedTypeVariable<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedType(atm.getUpperBound()),
                        getQualifiedType(atm.getLowerBound()));
            }

            public QualifiedTypeMirror<Q> visitUnion(AnnotatedUnionType atm, Q qual) {
                return new QualifiedUnionType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedTypeList(atm.getAlternatives()));
            }

            public QualifiedTypeMirror<Q> visitWildcard(AnnotatedWildcardType atm, Q qual) {
                return new QualifiedWildcardType<Q>(
                        WrappedAnnotatedTypeMirror.wrap(atm),
                        qual,
                        getQualifiedType(atm.getExtendsBound()),
                        getQualifiedType(atm.getSuperBound()));
            }
        };


    /* Conversion functions between qualifiers and @Key AnnotationMirrors */

    /** Get the qualifier corresponding to a Key annotation.
     */
    public Q getQualifier(AnnotationMirror anno) {
        if (anno == null) {
            return null;
        }
        int index = getIndex(anno);
        return indexToQual.get(index);
    }

    /** Get the qualifier corresponding to the Key annotation present on an
     * AnnotatedTypeMirror, or null if no such annotation exists.
     */
    public Q getQualifier(AnnotatedTypeMirror atm) {
        return getQualifier(atm.getAnnotation(Key.class));
    }

    /** Get an AnnotationMirror for a Key annotation encoding the specified
     * qualifier.
     */
    public AnnotationMirror getAnnotation(Q qual) {
        return createKey(getIndexForQualifier(qual), qual);
    }


    /* Miscellaneous utility functions */

    /** Check if an AnnotationMirror is a valid Key annotation.
     */
    public boolean isKey(AnnotationMirror anno) {
        // TODO: this should check that 'anno' has a valid index.
        return anno != null && blankKey.getAnnotationType().equals(anno.getAnnotationType());
    }

    /** Get a Key annotation containing no index.
     */
    public AnnotationMirror getBlankKeyAnnotation() {
        return blankKey;
    }
}
