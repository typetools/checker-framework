package org.checkerframework.common.initializedfields;

import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.initializedfields.qual.EnsuresInitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFieldsBottom;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.Contract;
import org.checkerframework.framework.util.ContractsFromMethod;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.UserError;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/** The annotated type factory for the Initialized Fields Checker. */
public class InitializedFieldsAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

    /**
     * The type factories that determine whether the default value is consistent with the annotated
     * type. If empty, warn about all uninitialized fields.
     */
    List<GenericAnnotatedTypeFactory<?, ?, ?, ?>> defaultValueAtypeFactories;

    /**
     * Creates a new InitializedFieldsAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    public InitializedFieldsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, InitializedFields.class, InitializedFieldsBottom.class);

        String[] checkerNames = getCheckerNames();

        defaultValueAtypeFactories = new ArrayList<>();
        for (String checkerName : checkerNames) {
            if (checkerName.equals(InitializedFieldsChecker.class.getCanonicalName())) {
                continue;
            }
            @SuppressWarnings("signature:argument.type.incompatible") // -processor is a binary name
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atf = getTypeFactory(checkerName);
            if (atf != null) {
                defaultValueAtypeFactories.add(atf);
            }
        }

        this.postInit();
    }

    /**
     * Returns the type factory for the given annotation processor, if it is type-checker.
     *
     * @param processorName the fully-qualified class name of an annotation processor
     * @return the type factory for the given annotation processor, or null if it's not a checker
     */
    GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory(@BinaryName String processorName) {
        try {
            Class<?> checkerClass = Class.forName(processorName);
            if (!BaseTypeChecker.class.isAssignableFrom(checkerClass)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            BaseTypeChecker c =
                    ((Class<? extends BaseTypeChecker>) checkerClass)
                            .getDeclaredConstructor()
                            .newInstance();
            c.init(processingEnv);
            c.initChecker();
            BaseTypeVisitor<?> v = c.createSourceVisitorPublic();
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atf = v.createTypeFactoryPublic();
            if (atf == null) {
                throw new UserError(
                        "Cannot find %s; check the classpath or processorpath", processorName);
            }
            return atf;
        } catch (ClassNotFoundException
                | InstantiationException
                | InvocationTargetException
                | IllegalAccessException
                | NoSuchMethodException e) {
            throw new UserError("Problem instantiating " + processorName, e);
        }
    }

    @Override
    public InitializedFieldsContractsFromMethod getContractsFromMethod() {
        return new InitializedFieldsContractsFromMethod(this);
    }

    /**
     * A subclass of ContractsFromMethod that adds a postcondition contract to each constructor,
     * requiring that it initializes all fields.
     */
    private class InitializedFieldsContractsFromMethod extends ContractsFromMethod {
        /**
         * Creates an InitializedFieldsContractsFromMethod for the given factory.
         *
         * @param factory the type factory associated with the newly-created ContractsFromMethod
         */
        public InitializedFieldsContractsFromMethod(
                GenericAnnotatedTypeFactory<?, ?, ?, ?> factory) {
            super(factory);
        }

        @Override
        public Set<Contract.Postcondition> getPostconditions(ExecutableElement executableElement) {
            Set<Contract.Postcondition> result = super.getPostconditions(executableElement);

            // Only process methods defined in source code being type-checked.
            if (declarationFromElement(executableElement) != null) {

                if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
                    // It's a constructor

                    String[] fieldsToInitialize =
                            fieldsToInitialize(
                                    (TypeElement) executableElement.getEnclosingElement());
                    if (fieldsToInitialize.length != 0) {

                        AnnotationMirror initializedFieldsAnno;
                        {
                            AnnotationBuilder builder =
                                    new AnnotationBuilder(processingEnv, InitializedFields.class);
                            builder.setValue("value", fieldsToInitialize);
                            initializedFieldsAnno = builder.build();
                        }
                        AnnotationMirror ensuresAnno;
                        {
                            AnnotationBuilder builder =
                                    new AnnotationBuilder(
                                            processingEnv, EnsuresInitializedFields.class);
                            builder.setValue("value", new String[] {"this"});
                            builder.setValue("fields", fieldsToInitialize);
                            ensuresAnno = builder.build();
                        }
                        Contract.Postcondition ensuresContract =
                                new Contract.Postcondition(
                                        "this", initializedFieldsAnno, ensuresAnno);

                        result.add(ensuresContract);
                    }
                }
            }

            return result;
        }
    }

    /**
     * Returns the fields that the constructor must initialize. These are the fields F declared in
     * this class that satisfy all of the following conditions:
     *
     * <ul>
     *   <li>F is a non-final field (if final, Java will issue a warning, so we don't need to).
     *   <li>F's declaration has no initializer.
     *   <li>No initialization block or static initialization block sets the field. (This is handled
     *       automatically because dataflow visits (static) initialization blocks as part of the
     *       constructor.)
     *   <li>F's annotated type is not consistent with the default value (0, 0.0, false, or null)
     * </ul>
     *
     * @param type the type whose fields to list
     * @return the fields whose type is not consistent with the default value, so the constructor
     *     must initialize them
     */
    // It is a bit wasteful that this is recomputed for each constructor.
    private String[] fieldsToInitialize(TypeElement type) {
        List<String> result = new ArrayList<String>();

        for (Element member : type.getEnclosedElements()) {

            if (member.getKind() != ElementKind.FIELD) {
                continue;
            }

            VariableElement field = (VariableElement) member;
            if (ElementUtils.isFinal(field)) {
                continue;
            }

            VariableTree fieldTree = (VariableTree) declarationFromElement(field);
            if (fieldTree.getInitializer() != null) {
                continue;
            }

            if (!defaultValueIsOK(field)) {
                result.add(field.getSimpleName().toString());
            }
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Returns true if the default field value (0, false, or null) is consistent with the field's
     * declared type.
     *
     * @param field a field
     * @return true if the default field value is consistent with the field's declared type
     */
    private boolean defaultValueIsOK(VariableElement field) {
        if (defaultValueAtypeFactories.isEmpty()) {
            return false;
        }

        for (GenericAnnotatedTypeFactory<?, ?, ?, ?> defaultValueAtypeFactory :
                defaultValueAtypeFactories) {
            defaultValueAtypeFactory.setRoot(root);

            AnnotatedTypeMirror fieldType = defaultValueAtypeFactory.getAnnotatedType(field);
            AnnotatedTypeMirror defaultValueType =
                    defaultValueAtypeFactory.getDefaultValueAnnotatedType(
                            fieldType.getUnderlyingType());
            if (!defaultValueAtypeFactory
                    .getTypeHierarchy()
                    .isSubtype(defaultValueType, fieldType)) {
                return false;
            }
        }

        return true;
    }
}
