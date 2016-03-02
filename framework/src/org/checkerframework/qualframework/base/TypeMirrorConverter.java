package org.checkerframework.qualframework.base;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedArrayType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedIntersectionType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedNoType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedNullType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedParameterDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedPrimitiveType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedUnionType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedWildcardType;
import org.checkerframework.qualframework.util.WrappedAnnotatedTypeMirror;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

/**
 * Helper class used by adapters to convert between {@link QualifiedTypeMirror}
 * and {@link AnnotatedTypeMirror}.
 *
 * Only adapters should ever have a reference to this class.  All adapters for
 * a single type system must use the same {@link TypeMirrorConverter} instance,
 * since converting from {@link QualifiedTypeMirror} to {@link
 * AnnotatedTypeMirror} and back will fail if the two conversion steps are
 * performed with different instances.
 */
/* This class uses a lookup table and a special annotation '@Key' to encode
 * qualifiers as annotations.  Each '@Key' annotation contains a single index,
 * which is a key into the lookup table indicating a particular qualifier.
 */
public class TypeMirrorConverter<Q> {
    /** The checker adapter, used for lazy initialization of {@link
     * typeFactory}. */
    private final CheckerAdapter<Q> checkerAdapter;
    /** Annotation processing environment, used to construct new {@link Key}
     * {@link AnnotationMirror}s. */
    private final ProcessingEnvironment processingEnv;
    /** The {@link Element} corresponding to the {@link Key#index()} field. */
    private final ExecutableElement indexElement;
    /** A {@link Key} annotation with no <code>index</code> set. */
    private final AnnotationMirror blankKey;
    /** The type factory adapter, used to construct {@link
     * AnnotatedTypeMirror}s. */
    private QualifiedTypeFactoryAdapter<Q> typeFactory;

    /** The next unused index in the lookup table. */
    private int nextIndex = 0;

    /** The qualifier-to-index half of the lookup table.  This lets us ensure
     * that the same qualifier maps to the same <code>@Key</code> annotation.
     */
    private final HashMap<Q, Integer> qualToIndex;
    /** The index-to-qualifier half of the lookup table.  This is used for
     * annotated-to-qualified conversions. */
    private final HashMap<Integer, Q> indexToQual;

    /** Cache @Key annotation mirrors so they are not to be recreated on every conversion */
    public LinkedHashMap<Integer, AnnotationMirror> keyToAnnoCache = new LinkedHashMap<Integer, AnnotationMirror>(10, .75f, true) {
         private static final long serialVersionUID = 1L;
         private static final int MAX_SIZE = 1000;
         @Override
         protected boolean removeEldestEntry(Map.Entry<Integer, AnnotationMirror> eldest) {
            return size() > MAX_SIZE;
         }
    };

    @SubtypeOf({})
    public static @interface Key {
        /** An index into the lookup table. */
        int index() default -1;
        /** A string representation of the qualifier this {@link Key}
         * represents.  This lets us have slightly nicer error messages. */
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

    /** Returns the type factory to use for building {@link
     * AnnotatedTypeMirror}s, running lazy initialization if necessary. */
    private QualifiedTypeFactoryAdapter<Q> getTypeFactory() {
        if (typeFactory == null) {
            typeFactory = checkerAdapter.getTypeFactory();
        }
        return typeFactory;
    }

    /** Constructs a new {@link Key} annotation with the provided index, using
     * <code>desc.toString()</code> to set the {@link Key#desc()} field. */
    private AnnotationMirror createKey(int index, Object desc) {
        if (keyToAnnoCache.containsKey(index)) {
            return keyToAnnoCache.get(index);
        } else {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Key.class.getCanonicalName());
            builder.setValue("index", index);
            builder.setValue("desc", "" + desc);
            AnnotationMirror result = builder.build();
            keyToAnnoCache.put(index, result);
            return result;
        }
    }

    /** Returns the index that represents <code>qual</code>.  If
     * <code>qual</code> has not been assigned an index yet, a new index will
     * be generated and assigned to it.
     */
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

    /** Returns the <code>index</code> field of a {@link Key} {@link
     * AnnotationMirror}. */
    private int getIndex(AnnotationMirror anno) {
        return getAnnotationField(anno, indexElement);
    }

    /** Helper function to obtain an integer field value from an {@link
     * AnnotationMirror}. */
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

        atm.clearAnnotations();

        // Only add the qualifier if it is not null,
        // and the original underlying ATV had a primary annotation.
        if (qtm.getQualifier() != null
            && (qtm.getKind() != TypeKind.TYPEVAR
                || ((QualifiedTypeVariable<Q>)qtm).isPrimaryQualifierValid())) {

            // Apply the qualifier for this QTM-ATM pair.
            int index = getIndexForQualifier(qtm.getQualifier());
            AnnotationMirror key = createKey(index, qtm.getQualifier());

            atm.addAnnotation(key);
        }

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
        AnnotatedTypeMirror atm;
        if (qtm.getUnderlyingType() instanceof WrappedAnnotatedTypeMirror) {
            atm = ((WrappedAnnotatedTypeMirror)qtm.getUnderlyingType()).unwrap().deepCopy();
        } else {
            atm = AnnotatedTypeMirror.createType(
                qtm.getUnderlyingType().getOriginalType(), getTypeFactory(),
                qtm.getUnderlyingType().isDeclaration());
        }
        applyQualifiers(qtm, atm);
        return atm;
    }

    public List<AnnotatedTypeMirror> getAnnotatedTypeList(List<? extends QualifiedTypeMirror<Q>> qtms) {
        if (qtms == null) {
            return null;
        }

        List<AnnotatedTypeMirror> atms = new ArrayList<AnnotatedTypeMirror>();
        for (QualifiedTypeMirror<Q> qtm : qtms) {
            atms.add(getAnnotatedType(qtm));
        }
        return atms;
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
    private final SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror> APPLY_COMPONENT_QUALIFIERS_VISITOR =
        new SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror>() {
            private final IdentityHashMap<AnnotatedTypeMirror, Void> seenATVs = new IdentityHashMap<>();

            @Override
            public Void visitArray(QualifiedArrayType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedArrayType atm = (AnnotatedArrayType)rawAtm;
                applyQualifiers(qtm.getComponentType(), atm.getComponentType());
                return null;
            }

            @Override
            public Void visitDeclared(QualifiedDeclaredType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedDeclaredType atm = (AnnotatedDeclaredType)rawAtm;
                applyQualifiersToLists(qtm.getTypeArguments(), atm.getTypeArguments());
                return null;
            }

            @Override
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
                applyQualifiersToLists(qtm.getTypeParameters(), atm.getTypeVariables());
                return null;
            }

            @Override
            public Void visitIntersection(QualifiedIntersectionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedIntersectionType atm = (AnnotatedIntersectionType)rawAtm;
                applyQualifiersToLists(qtm.getBounds(), atm.directSuperTypes());
                return null;
            }

            @Override
            public Void visitNoType(QualifiedNoType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // NoType has no components.
                return null;
            }

            @Override
            public Void visitNull(QualifiedNullType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // NullType has no components.
                return null;
            }

            @Override
            public Void visitPrimitive(QualifiedPrimitiveType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                // PrimitiveType has no components.
                return null;
            }

            @Override
            public Void visitTypeVariable(QualifiedTypeVariable<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedTypeVariable atm = (AnnotatedTypeVariable)rawAtm;
                typeVariableHelper(qtm.getDeclaration(), atm);
                return null;
            }

            @Override
            public Void visitUnion(QualifiedUnionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedUnionType atm = (AnnotatedUnionType)rawAtm;
                applyQualifiersToLists(qtm.getAlternatives(), atm.getAlternatives());
                return null;
            }

            @Override
            public Void visitWildcard(QualifiedWildcardType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedWildcardType atm = (AnnotatedWildcardType)rawAtm;
                applyQualifiers(qtm.getExtendsBound(), atm.getExtendsBound());
                applyQualifiers(qtm.getSuperBound(), atm.getSuperBound());
                return null;
            }

            private void typeVariableHelper(QualifiedParameterDeclaration<Q> qtm, AnnotatedTypeVariable atm) {
                if (!seenATVs.containsKey(atm)) {
                    seenATVs.put(atm, null);
                    QualifiedTypeParameterBounds<Q> bounds =
                        getTypeFactory().getUnderlying()
                            .getQualifiedTypeParameterBounds(qtm.getUnderlyingType());
                    try {
                        applyQualifiers(bounds.getUpperBound(), atm.getUpperBound());
                        applyQualifiers(bounds.getLowerBound(), atm.getLowerBound());
                    } finally {
                        seenATVs.remove(atm);
                    }
                }
            }

            @Override
            public Void visitParameterDeclaration(QualifiedParameterDeclaration<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedTypeVariable atm = (AnnotatedTypeVariable)rawAtm;
                typeVariableHelper(qtm, atm);
                return null;
            }

            @Override
            public Void visitTypeDeclaration(QualifiedTypeDeclaration<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedDeclaredType atm = (AnnotatedDeclaredType)rawAtm;
                applyQualifiersToLists(qtm.getTypeParameters(), atm.getTypeArguments());
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
        return getQualifiedTypeFromWrapped(WrappedAnnotatedTypeMirror.wrap(atm));
    }

    public List<QualifiedTypeMirror<Q>> getQualifiedTypeList(List<? extends AnnotatedTypeMirror> atms) {
        if (atms == null) {
            return null;
        }

        List<QualifiedTypeMirror<Q>> qtms = new ArrayList<>();
        for (AnnotatedTypeMirror atm : atms) {
            qtms.add(getQualifiedType(atm));
        }
        return qtms;
    }

    private QualifiedTypeMirror<Q> getQualifiedTypeFromWrapped(WrappedAnnotatedTypeMirror watm) {
        if (watm == null) {
            return null;
        }
        // FIXME: This is a total hack to work around a particularly nasty
        // aspect of the framework's AnnotatedTypeVariable handling.
        //
        // Consider:
        //      class C<T> {
        //          class D<U extends T> { }
        //      }
        //
        // When processing the declaration of `U`, the underlying
        // AnnotatedTypeMirror initially has no @Key annotations.  During
        // processing, TypeMirrorConverter will request the qualified
        // bounds of `U`.  This leads to a call chain: ATV.getLowerBound ->
        // fixupBoundAnnotations -> getUpperBound().getEffectiveAnnotations() ->
        // getEffectiveUpperBound().  This results in calling substitute (and
        // therefore postTypeVarSubstitution) on the upper bound, which
        // currently has no annotations.  So the ATM -> QTM conversion fails
        // ("can't create QTM with null qualifier").  Using TypeAnnotator
        // instead of UPDATED_QTM_BUILDER is not a great solution (since we end
        // up running TypeAnnotator on the same type more than once) but it does
        // work around this issue.
        //
        // TODO: This also works around the problem of capture-converted
        // wildcards starting out with no top-level annotation.  This is another
        // one we should fix properly instead of hacking around.
        DefaultQualifiedTypeFactory<Q> defaultFactory = (DefaultQualifiedTypeFactory<Q>)getTypeFactory().getUnderlying();
        return watm.accept(defaultFactory.getTypeAnnotator(), null);
        // UPDATED_QTM_BUILDER was deleted in the same commit that added the
        // workaround.
        //return watm.accept(UPDATED_QTM_BUILDER, getQualifier(watm.unwrap()));
    }

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
        // TODO: This should probably also check that 'anno' has a value in its
        // 'index' field.
        return anno != null && blankKey.getAnnotationType().equals(anno.getAnnotationType());
    }

    /** Get a Key annotation containing no index.
     */
    public AnnotationMirror getBlankKeyAnnotation() {
        return blankKey;
    }
}
