package org.checkerframework.common.wholeprograminference.scenelib;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.SceneToStubWriter;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage.AnnotationsInContexts;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.UserError;
import scenelib.annotations.Annotation;
import scenelib.annotations.el.AClass;
import scenelib.annotations.el.AField;
import scenelib.annotations.el.AMethod;
import scenelib.annotations.el.AScene;
import scenelib.annotations.el.ATypeElement;
import scenelib.annotations.el.DefException;
import scenelib.annotations.io.IndexFileWriter;

/**
 * scene-lib (from the Annotation File Utilities) doesn't provide enough information to usefully
 * print stub files: it lacks information about what is and is not an enum, about the base types of
 * variables, and about formal parameter names.
 *
 * <p>This class wraps AScene but provides access to that missing information. This allows us to
 * preserve the code that generates .jaif files, while allowing us to sanely and safely keep the
 * information we need to generate stubs.
 *
 * <p>This class would be better as a subclass of AScene.
 */
public class ASceneWrapper {

    /** The AScene being wrapped. */
    private final AScene theScene;

    /**
     * Constructor. Pass the AScene to wrap.
     *
     * @param theScene the scene to wrap
     */
    public ASceneWrapper(AScene theScene) {
        this.theScene = theScene;
    }

    /**
     * Fetch the classes in this scene, represented as AClass objects.
     *
     * @return an immutable map from binary names to AClass objects
     */
    @SuppressWarnings("signature:return.type.incompatible") // unannotated ImmutableMap??
    public Map<@BinaryName String, AClass> getClasses() {
        return ImmutableMap.copyOf(theScene.classes);
    }

    /**
     * Removes the specified annotations from an AScene.
     *
     * @param scene the scene from which to remove annotations
     * @param annosToRemove annotations that should not be added to .jaif or stub files
     */
    private void removeAnnosFromScene(AScene scene, AnnotationsInContexts annosToRemove) {
        for (AClass aclass : scene.classes.values()) {
            for (AField field : aclass.fields.values()) {
                removeAnnosFromATypeElement(field.type, TypeUseLocation.FIELD, annosToRemove);
            }
            for (AMethod method : aclass.methods.values()) {
                removeAnnosFromATypeElement(
                        method.returnType, TypeUseLocation.RETURN, annosToRemove);
                removeAnnosFromATypeElement(
                        method.receiver.type, TypeUseLocation.RECEIVER, annosToRemove);
                for (AField param : method.parameters.values()) {
                    removeAnnosFromATypeElement(
                            param.type, TypeUseLocation.PARAMETER, annosToRemove);
                }
            }
        }
    }

    /**
     * Removes the specified annotations from an ATypeElement.
     *
     * @param typeElt the type element from which to remove annotations
     * @param loc the location where typeElt is used
     * @param annosToRemove annotations that should not be added to .jaif or stub files
     */
    private void removeAnnosFromATypeElement(
            ATypeElement typeElt, TypeUseLocation loc, AnnotationsInContexts annosToRemove) {
        String annosToRemoveKey = typeElt.description.toString() + typeElt.tlAnnotationsHere;
        Set<String> annosToRemoveForLocation = annosToRemove.get(Pair.of(annosToRemoveKey, loc));
        if (annosToRemoveForLocation != null) {
            Set<Annotation> annosToRemoveHere = new HashSet<>();
            for (Annotation anno : typeElt.tlAnnotationsHere) {
                if (annosToRemoveForLocation.contains(anno.def().toString())) {
                    annosToRemoveHere.add(anno);
                }
            }
            typeElt.tlAnnotationsHere.removeAll(annosToRemoveHere);
        }

        // Recursively remove annotations from inner types
        for (ATypeElement innerType : typeElt.innerTypes.values()) {
            removeAnnosFromATypeElement(innerType, loc, annosToRemove);
        }
    }

    /**
     * Write the scene wrapped by this object to a file at the given path.
     *
     * @param jaifPath the path of the file to be written, but ending in ".jaif". If {@code
     *     outputformat} is not {@code JAIF}, the path will be modified to match.
     * @param annosToIgnore which annotations should be ignored in which contexts
     * @param outputFormat the output format to use
     * @param checker the checker from which this method is called, for naming stub files
     */
    public void writeToFile(
            String jaifPath,
            AnnotationsInContexts annosToIgnore,
            OutputFormat outputFormat,
            BaseTypeChecker checker) {
        assert jaifPath.endsWith(".jaif");
        AScene scene = theScene.clone();
        removeAnnosFromScene(scene, annosToIgnore);
        scene.prune();
        String filepath;
        switch (outputFormat) {
            case JAIF:
                filepath = jaifPath;
                break;
            case STUB:
                String astubWithChecker = "-" + checker.getClass().getCanonicalName() + ".astub";
                filepath = jaifPath.replace(".jaif", astubWithChecker);
                break;
            default:
                throw new BugInCF("Unhandled outputFormat " + outputFormat);
        }
        new File(filepath).delete();
        if (!scene.isEmpty()) {
            // Only write non-empty scenes into files.
            try {
                switch (outputFormat) {
                    case STUB:
                        SceneToStubWriter.write(this, filepath);
                        break;
                    case JAIF:
                        IndexFileWriter.write(scene, new FileWriter(filepath));
                        break;
                    default:
                        throw new BugInCF("Unhandled outputFormat " + outputFormat);
                }
            } catch (IOException e) {
                throw new UserError("Problem while writing %s: %s", filepath, e.getMessage());
            } catch (DefException e) {
                throw new BugInCF(e);
            }
        }
    }

    /**
     * Obtain the given class, which can be further operated on to e.g. add information about a
     * method. This method also updates the symbol information stored about the class using the
     * given ClassSymbol, if it is non-null.
     *
     * <p>Results are interned.
     *
     * @param className the binary name of the class to be added to the scene
     * @param classSymbol the element representing the class, used for adding symbol information to
     *     the AClass returned by this method. If it is null, an AClass is looked up or created, but
     *     the symbol information stored by the AClass is not updated.
     * @return an AClass representing that class
     */
    public AClass vivifyClass(@BinaryName String className, @Nullable ClassSymbol classSymbol) {
        AClass aClass = theScene.classes.getVivify(className);

        // updateSymbolInformation must be called on both paths (cache hit and cache miss) because
        // the
        // second parameter could have been null when the first miss occurred.
        // Different visit methods in CFAbstractTransfer call WPI in different ways.  Only some
        // provide the symbol information, and the visit order isn't known ahead of time.
        // Since it is not used until the end of WPI, it being unavailable during WPI is not a
        // problem.
        if (classSymbol != null) {
            updateSymbolInformation(aClass, classSymbol);
        }
        return aClass;
    }

    /**
     * Updates the symbol information stored in AClass for the given class.
     *
     * @param aClass the class representation in which the symbol information is to be updated
     * @param classSymbol the source of the symbol information
     */
    private void updateSymbolInformation(AClass aClass, ClassSymbol classSymbol) {
        if (classSymbol.isEnum()) {
            List<VariableElement> enumConstants = new ArrayList<>();
            for (Element e : ((TypeElement) classSymbol).getEnclosedElements()) {
                if (e.getKind() == ElementKind.ENUM_CONSTANT) {
                    enumConstants.add((VariableElement) e);
                }
            }
            // Either call setEnumConstants or verify that the existing value is consistent.
            if (!aClass.isEnum(classSymbol.getSimpleName().toString())) {
                aClass.setEnumConstants(enumConstants);
            } else {
                List<VariableElement> existingEnumConstants = aClass.getEnumConstants();
                if (existingEnumConstants.size() != enumConstants.size()) {
                    throw new BugInCF(
                            "inconsistent enum constants in WPI for class "
                                    + classSymbol.getQualifiedName().toString());
                }
                for (int i = 0; i < enumConstants.size(); i++) {
                    if (!existingEnumConstants.get(i).equals(enumConstants.get(i))) {
                        throw new BugInCF(
                                "inconsistent enum constants in WPI for class "
                                        + classSymbol.getQualifiedName().toString());
                    }
                }
            }
        }

        ClassSymbol outerClass = classSymbol;
        ClassSymbol previous = classSymbol;
        do {
            if (outerClass.isEnum()) {
                aClass.markAsEnum(outerClass.getSimpleName().toString());
            }
            Element element = classSymbol.getEnclosingElement();
            if (element == null || element.getKind() == ElementKind.PACKAGE) {
                break;
            }
            TypeElement t = ElementUtils.enclosingClass(element);
            previous = outerClass;
            outerClass = (ClassSymbol) t;
            // It is necessary to check that previous isn't equal to outer class because
            // otherwise this loop will sometimes run forever.
        } while (outerClass != null && !previous.equals(outerClass));

        aClass.setTypeElement(classSymbol);
    }

    /**
     * Avoid using this if possible; use the other methods of this class unless you absolutely need
     * an AScene.
     *
     * @return the AScene representation of this
     */
    public AScene getAScene() {
        return theScene;
    }
}
