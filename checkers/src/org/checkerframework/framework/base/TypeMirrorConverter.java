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

class TypeMirrorConverter<Q> {
    private ProcessingEnvironment processingEnv;
    private ExecutableElement qualIndexElement;
    private ExecutableElement typeIndexElement;
    private AnnotationMirror blankKey;
    private AnnotatedTypeFactory dummyATF;

    private int nextIndex = 0;

    private HashMap<Q, Integer> qualToIndex;
    private HashMap<Integer, Q> indexToQual;
    private HashMap<QualifiedTypeMirror<Q>, Integer> qualTypeToIndex;
    private HashMap<Integer, QualifiedTypeMirror<Q>> indexToQualType;
    private HashMap<QualifiedTypeMirror<Q>, AnnotatedTypeMirror> qualTypeToAnnoType;

    @TypeQualifier
    @SubtypeOf({})
    public static @interface Key {
        int qualIndex() default -1;
        int typeIndex() default -1;
        String desc() default "";
    }


    public TypeMirrorConverter(ProcessingEnvironment processingEnv, CheckerAdapter<Q> checkerAdapter) {
        this.processingEnv = processingEnv;
        this.qualIndexElement = TreeUtils.getMethod(Key.class.getCanonicalName(), "qualIndex", 0, processingEnv);
        this.typeIndexElement = TreeUtils.getMethod(Key.class.getCanonicalName(), "typeIndex", 0, processingEnv);
        this.blankKey = AnnotationUtils.fromClass(processingEnv.getElementUtils(), Key.class);
        this.dummyATF = new BaseAnnotatedTypeFactory(checkerAdapter) {
            @Override
            public boolean isSupportedQualifier(AnnotationMirror anno) {
                return true;
            }
        };

        this.qualToIndex = new HashMap<>();
        this.indexToQual = new HashMap<>();
        this.qualTypeToIndex = new HashMap<>();
        this.indexToQualType = new HashMap<>();
        this.qualTypeToAnnoType = new HashMap<>();
    }

    private AnnotationMirror createKey(int qualIndex, int typeIndex, Object desc) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Key.class.getCanonicalName());
        builder.setValue("qualIndex", qualIndex);
        builder.setValue("typeIndex", typeIndex);
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

    private int getIndexForType(QualifiedTypeMirror<Q> qtm) {
        if (qualTypeToIndex.containsKey(qtm)) {
            return qualTypeToIndex.get(qtm);
        } else {
            int index = nextIndex++;
            qualTypeToIndex.put(qtm, index);
            indexToQualType.put(index, qtm);
            return index;
        }
    }

    private int getQualifierIndex(AnnotationMirror anno) {
        return getAnnotationField(anno, qualIndexElement);
    }

    private int getTypeIndex(AnnotationMirror anno) {
        return getAnnotationField(anno, typeIndexElement);
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


    public void bindTypes(QualifiedTypeMirror<Q> qtm, AnnotatedTypeMirror atm) {
        if (qtm == null && atm == null) {
            return;
        }
        if (qtm != null) {
            // Recursively create entries for all component QTM-ATM pairs.
            BIND_TYPE_COMPONENTS_VISITOR.visit(qtm, atm);
        }

        int qualIndex = getIndexForQualifier(qtm.getQualifier());
        int typeIndex = getIndexForType(qtm);

        // Create entries for this QTM-ATM pair.
        AnnotationMirror key = createKey(qualIndex, typeIndex, qtm.getQualifier());

        atm.clearAnnotations();
        atm.addAnnotation(key);
        qualTypeToAnnoType.put(qtm, atm);
    }

    private void bindTypeLists(List<? extends QualifiedTypeMirror<Q>> qtms, List<? extends AnnotatedTypeMirror> atms) {
        assert(qtms.size() == atms.size());
        for (int i = 0; i < qtms.size(); ++i) {
            bindTypes(qtms.get(i), atms.get(i));
        }
    }

    private SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror> BIND_TYPE_COMPONENTS_VISITOR =
        new SimpleQualifiedTypeVisitor<Q, Void, AnnotatedTypeMirror>() {
            public Void visitArray(QualifiedArrayType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedArrayType atm = (AnnotatedArrayType)rawAtm;
                bindTypes(qtm.getComponentType(), atm.getComponentType());
                return null;
            }

            public Void visitDeclared(QualifiedDeclaredType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedDeclaredType atm = (AnnotatedDeclaredType)rawAtm;
                bindTypeLists(qtm.getTypeArguments(), atm.getTypeArguments());  
                return null;
            }

            public Void visitExecutable(QualifiedExecutableType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedExecutableType atm = (AnnotatedExecutableType)rawAtm;
                bindTypeLists(qtm.getParameterTypes(), atm.getParameterTypes());
                bindTypes(qtm.getReceiverType(), atm.getReceiverType());
                bindTypes(qtm.getReturnType(), atm.getReturnType());
                bindTypeLists(qtm.getThrownTypes(), atm.getThrownTypes());
                bindTypeLists(qtm.getTypeVariables(), atm.getTypeVariables());
                return null;
            }

            public Void visitIntersection(QualifiedIntersectionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedIntersectionType atm = (AnnotatedIntersectionType)rawAtm;
                bindTypeLists(qtm.getBounds(), atm.directSuperTypes());
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
                bindTypes(qtm.getLowerBound(), atm.getLowerBound());
                bindTypes(qtm.getUpperBound(), atm.getUpperBound());
                return null;
            }

            public Void visitUnion(QualifiedUnionType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedUnionType atm = (AnnotatedUnionType)rawAtm;
                bindTypeLists(qtm.getAlternatives(), atm.getAlternatives());
                return null;
            }

            public Void visitWildcard(QualifiedWildcardType<Q> qtm, AnnotatedTypeMirror rawAtm) {
                AnnotatedWildcardType atm = (AnnotatedWildcardType)rawAtm;
                bindTypes(qtm.getExtendsBound(), atm.getExtendsBound());
                bindTypes(qtm.getSuperBound(), atm.getSuperBound());
                return null;
            }
        };


    /** Update the QualifiedTypeMirrors associated with an AnnotatedTypeMirror
     * to make them consistent with the AnnotatedTypeMirror's associated
     * qualifiers.
     *
     * @return true iff some part of the type needed an update
     */
    public boolean updateType(AnnotatedTypeMirror atm) {
        if (atm == null) {
            return false;
        }

        boolean needsUpdate = false;

        // Recursively update the ATM's components.
        if (UPDATE_TYPE_COMPONENTS_VISITOR.visit(atm)) {
            // Some component needed an update.
            needsUpdate = true;
        }

        AnnotationMirror key = atm.getAnnotation(Key.class);
        if (key == null) {
            // If it has no @Key, we have nothing to do.
            return needsUpdate;
        }

        int qualIndex = getQualifierIndex(key);
        int typeIndex = getTypeIndex(key);
        if (typeIndex == -1) {
            needsUpdate = true;
        } else {
            // If it has a Key, the Key should have a valid qualIndex.
            assert qualIndex != -1 : "Key.qualIndex should always be a valid index";
            if (indexToQualType.get(typeIndex).getQualifier() !=
                    indexToQual.get(qualIndex)) {
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
            Q qual = indexToQual.get(qualIndex);
            QualifiedTypeMirror<Q> qtm = UPDATED_QTM_BUILDER.visit(atm, qual);

            typeIndex = getIndexForType(qtm);
            key = createKey(qualIndex, typeIndex, qual);
            atm.clearAnnotations();
            atm.addAnnotation(key);
            qualTypeToAnnoType.put(qtm, atm);
        }
        return needsUpdate;
    }

    private boolean updateTypeList(List<? extends AnnotatedTypeMirror> atms) {
        boolean result = false;
        for (int i = 0; i < atms.size(); ++i) {
            if (updateType(atms.get(i))) {
                result = true;
            }
        }
        return result;
    }

    private SimpleAnnotatedTypeVisitor<Boolean, Void> UPDATE_TYPE_COMPONENTS_VISITOR =
        new SimpleAnnotatedTypeVisitor<Boolean, Void>() {
            public Boolean visitArray(AnnotatedArrayType atm, Void v) {
                return updateType(atm.getComponentType());
            }

            public Boolean visitDeclared(AnnotatedDeclaredType atm, Void v) {
                return updateTypeList(atm.getTypeArguments());  
            }

            public Boolean visitExecutable(AnnotatedExecutableType atm, Void v) {
                return updateTypeList(atm.getParameterTypes())
                    || updateType(atm.getReceiverType())
                    || updateType(atm.getReturnType())
                    || updateTypeList(atm.getThrownTypes())
                    || updateTypeList(atm.getTypeVariables());
            }

            public Boolean visitIntersection(AnnotatedIntersectionType atm, Void v) {
                return updateTypeList(atm.directSuperTypes());
            }

            public Boolean visitNoType(AnnotatedNoType atm, Void v) {
                // NoType has no components.
                return false;
            }

            public Boolean visitNull(AnnotatedNullType atm, Void v) {
                // NullType has no components.
                return false;
            }

            public Boolean visitPrimitive(AnnotatedPrimitiveType atm, Void v) {
                // PrimitiveType has no components.
                return false;
            }

            public Boolean visitTypeVariable(AnnotatedTypeVariable atm, Void v) {
                return updateType(atm.getLowerBound())
                    || updateType(atm.getUpperBound());
            }

            public Boolean visitUnion(AnnotatedUnionType atm, Void v) {
                return updateTypeList(atm.getAlternatives());
            }

            public Boolean visitWildcard(AnnotatedWildcardType atm, Void v) {
                return updateType(atm.getExtendsBound())
                    || updateType(atm.getSuperBound());
            }
        };

    List<QualifiedTypeMirror<Q>> getQualifiedTypeList(List<? extends AnnotatedTypeMirror> atms) {
        List<QualifiedTypeMirror<Q>> result = new ArrayList<>();
        for (int i = 0; i < atms.size(); ++i) {
            result.add(getQualifiedType(atms.get(i)));
        }
        return result;
    }

    private SimpleAnnotatedTypeVisitor<QualifiedTypeMirror<Q>, Q> UPDATED_QTM_BUILDER =
        new SimpleAnnotatedTypeVisitor<QualifiedTypeMirror<Q>, Q>() {
            public QualifiedTypeMirror<Q> visitArray(AnnotatedArrayType atm, Q qual) {
                return new QualifiedArrayType<Q>(atm.getUnderlyingType(), qual,
                        getQualifiedType(atm.getComponentType()));
            }

            public QualifiedTypeMirror<Q> visitDeclared(AnnotatedDeclaredType atm, Q qual) {
                return new QualifiedDeclaredType<Q>(atm.getUnderlyingType(), qual,
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

                return new QualifiedExecutableType<Q>(atm.getUnderlyingType(), qual,
                        getQualifiedTypeList(atm.getParameterTypes()),
                        getQualifiedType(atm.getReceiverType()),
                        getQualifiedType(atm.getReturnType()),
                        getQualifiedTypeList(atm.getThrownTypes()),
                        qualifiedTypeVariables);
            }

            public QualifiedTypeMirror<Q> visitIntersection(AnnotatedIntersectionType atm, Q qual) {
                return new QualifiedIntersectionType<Q>(atm.getUnderlyingType(), qual,
                        getQualifiedTypeList(atm.directSuperTypes()));
            }

            public QualifiedTypeMirror<Q> visitNoType(AnnotatedNoType atm, Q qual) {
                // NoType has no components.
                return new QualifiedNoType<Q>(atm.getUnderlyingType(), qual);
            }

            public QualifiedTypeMirror<Q> visitNull(AnnotatedNullType atm, Q qual) {
                // NullType has no components.
                return new QualifiedNullType<Q>(atm.getUnderlyingType(), qual);
            }

            public QualifiedTypeMirror<Q> visitPrimitive(AnnotatedPrimitiveType atm, Q qual) {
                // PrimitiveType has no components.
                return new QualifiedPrimitiveType<Q>(atm.getUnderlyingType(), qual);
            }

            public QualifiedTypeMirror<Q> visitTypeVariable(AnnotatedTypeVariable atm, Q qual) {
                return new QualifiedTypeVariable<Q>(atm.getUnderlyingType(), qual,
                        getQualifiedType(atm.getLowerBound()),
                        getQualifiedType(atm.getUpperBound()));
            }

            public QualifiedTypeMirror<Q> visitUnion(AnnotatedUnionType atm, Q qual) {
                return new QualifiedUnionType<Q>(atm.getUnderlyingType(), qual,
                        getQualifiedTypeList(atm.getAlternatives()));
            }

            public QualifiedTypeMirror<Q> visitWildcard(AnnotatedWildcardType atm, Q qual) {
                return new QualifiedWildcardType<Q>(atm.getUnderlyingType(), qual,
                        getQualifiedType(atm.getExtendsBound()),
                        getQualifiedType(atm.getSuperBound()));
            }
        };


    public QualifiedTypeMirror<Q> getQualifiedType(AnnotatedTypeMirror atm) {
        if (!atm.hasAnnotation(Key.class)) {
            return null;
        }

        int typeIndex = getTypeIndex(atm.getAnnotation(Key.class));
        if (typeIndex == -1) {
            // Side-effecting the input - ugh.  At least it's idempotent.
            updateType(atm);
            typeIndex = getTypeIndex(atm.getAnnotation(Key.class));
        }
        return indexToQualType.get(typeIndex);
    }



/*
    private AnnotationMirror createKey(Object desc) {
    }

    private int getIndex(AnnotationMirror anno) {
        AnnotationValue value = anno.getElementValues().get(indexElement);
        if (value == null) {
            throw new IllegalArgumentException("@Key annotation contains no index");
        }
        assert(value.getValue() instanceof Integer);
        Integer index = (Integer)value.getValue();
        return index;
    }


    private void insert(Q qual) {
        AnnotationMirror key = createKey(qual);
        int index = getIndex(key);

        qualToAnno.put(qual, key);
        indexToQual.put(index, qual);
    }

    private void insertType(QualifiedTypeMirror<Q> qtm) {
        AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(
                qtm.getUnderlyingType(), null);
        bindTypes(qtm, atm);
    }

    public void bindTypes(QualifiedTypeMirror<Q> qtm, AnnotatedTypeMirror atm) {
    }
*/


    public Q getQualifier(AnnotationMirror anno) {
        if (anno == null) {
            return null;
        }
        int index = getQualifierIndex(anno);
        return indexToQual.get(index);
    }

    public Q getQualifier(AnnotatedTypeMirror atm) {
        return getQualifier(atm.getAnnotation(Key.class));
    }

    public AnnotationMirror getAnnotation(Q qual) {
        return createKey(getIndexForQualifier(qual), -1, qual);
    }

    public AnnotatedTypeMirror getAnnotatedType(QualifiedTypeMirror<Q> qtm) {
        if (!qualTypeToAnnoType.containsKey(qtm)) {
            AnnotatedTypeMirror atm = AnnotatedTypeMirror.createType(
                    qtm.getUnderlyingType(), dummyATF);
            bindTypes(qtm, atm);
        }
        return qualTypeToAnnoType.get(qtm);
    }


    public boolean isKey(AnnotationMirror anno) {
        return anno != null && blankKey.getAnnotationType().equals(anno.getAnnotationType());
    }

    public AnnotationMirror getBlankKeyAnnotation() {
        return blankKey;
    }
}
