package org.checkerframework.checker.i18nformatter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatBottom;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;
import org.checkerframework.checker.i18nformatter.qual.I18nInvalidFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nUnknownFormat;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

/**
 * Adds {@link I18nFormat} to the type of tree, if it is a {@code String} or
 * {@code char} literal that represents a satisfiable format. The annotation's
 * value is set to be a list of appropriate {@link I18nConversionCategory}
 * values for every parameter of the format.
 *
 * It also creates a map from the provided translation file if exists. This map
 * will be used to get the corresponding value of a key when
 * {@link java.util.ResourceBundle#getString} method is invoked.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 * @author Siwakorn Srisakaokul
 */
public class I18nFormatterAnnotatedTypeFactory extends
        GenericAnnotatedTypeFactory<CFValue, CFStore, I18nFormatterTransfer, I18nFormatterAnalysis> {

    private final AnnotationMirror I18NFORMAT;
    private final AnnotationMirror I18NINVALIDFORMAT;
    private final AnnotationMirror I18NFORMATBOTTOM;
    private final AnnotationMirror I18NFORMATFOR;

    public final Map<String, String> translations;

    protected final I18nFormatterTreeUtil treeUtil;

    public I18nFormatterAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        I18NFORMAT = AnnotationUtils.fromClass(elements, I18nFormat.class);
        I18NINVALIDFORMAT = AnnotationUtils.fromClass(elements, I18nInvalidFormat.class);
        I18NFORMATBOTTOM = AnnotationUtils.fromClass(elements, I18nFormatBottom.class);
        I18NFORMATFOR = AnnotationUtils.fromClass(elements, I18nFormatFor.class);

        this.translations = Collections.unmodifiableMap(buildLookup());

        this.treeUtil = new I18nFormatterTreeUtil(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll(
                I18nUnknownFormat.class, I18nFormatBottom.class);
    }

    private Map<String, String> buildLookup() {
        Map<String, String> result = new HashMap<String, String>();

        if (checker.hasOption("propfiles")) {
            String names = checker.getOption("propfiles");
            String[] namesArr = names.split(":");

            if (namesArr == null) {
                System.err.println("Couldn't parse the properties files: <" + names + ">");
            } else {
                for (String name : namesArr) {
                    try {
                        Properties prop = new Properties();

                        InputStream in = null;

                        ClassLoader cl = this.getClass().getClassLoader();
                        if (cl == null) {
                            // the class loader is null if the system class
                            // loader was
                            // used
                            cl = ClassLoader.getSystemClassLoader();
                        }
                        in = cl.getResourceAsStream(name);

                        if (in == null) {
                            // if the classloader didn't manage to load the
                            // file, try
                            // whether a FileInputStream works. For absolute
                            // paths this
                            // might help.
                            try {
                                in = new FileInputStream(name);
                            } catch (FileNotFoundException e) {
                                // ignore
                            }
                        }

                        if (in == null) {
                            System.err.println("Couldn't find the properties file: " + name);
                            // report(Result.failure("propertykeychecker.filenotfound",
                            // name), null);
                            // return Collections.emptySet();
                            continue;
                        }

                        prop.load(in);

                        for (String key : prop.stringPropertyNames()) {
                            result.put(key, prop.getProperty(key));
                        }
                    } catch (Exception e) {
                        // TODO: is there a nicer way to report messages, that
                        // are not
                        // connected to an AST node?
                        // One cannot use report, because it needs a node.
                        System.err.println("Exception in PropertyKeyChecker.keysOfPropertyFile: " + e);
                        e.printStackTrace();
                    }
                }
            }
        }

        if (checker.hasOption("bundlenames")) {
            String bundleNames = checker.getOption("bundlenames");
            String[] namesArr = bundleNames.split(":");

            if (namesArr == null) {
                System.err.println("Couldn't parse the resource bundles: <" + bundleNames + ">");
            } else {
                for (String bundleName : namesArr) {
                    ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
                    if (bundle == null) {
                        System.err.println("Couldn't find the resource bundle: <" + bundleName + "> for locale <"
                                + Locale.getDefault() + ">");
                        continue;
                    }

                    for (String key : bundle.keySet()) {
                        result.put(key, bundle.getString(key));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new I18nFormatterQualifierHierarchy(factory);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new I18nFormatterTreeAnnotator(this)
        );
    }

    private class I18nFormatterTreeAnnotator extends TreeAnnotator {
        public I18nFormatterTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(I18NFORMAT)) {
                String format = null;
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    format = (String) tree.getValue();
                } else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    format = Character.toString((Character) tree.getValue());
                }
                if (format != null) {
                    AnnotationMirror anno;
                    try {
                        I18nConversionCategory[] cs = I18nFormatUtil.formatParameterCategories(format);
                        anno = I18nFormatterAnnotatedTypeFactory.this.treeUtil.categoriesToFormatAnnotation(cs);
                    } catch (IllegalArgumentException e) {
                        anno = I18nFormatterAnnotatedTypeFactory.this.treeUtil.exceptionToInvalidFormatAnnotation(e);
                    }
                    type.addAnnotation(anno);
                }
            }

            return super.visitLiteral(tree, type);
        }
    }

    class I18nFormatterQualifierHierarchy extends GraphQualifierHierarchy {

        public I18nFormatterQualifierHierarchy(MultiGraphFactory f) {
            super(f, I18NFORMATBOTTOM);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, I18NFORMAT)
                    && AnnotationUtils.areSameIgnoringValues(lhs, I18NFORMAT)) {

                I18nConversionCategory[] rhsArgTypes = treeUtil.formatAnnotationToCategories(rhs);
                I18nConversionCategory[] lhsArgTypes = treeUtil.formatAnnotationToCategories(lhs);

                if (rhsArgTypes.length > lhsArgTypes.length) {
                    return false;
                }

                for (int i = 0; i < rhsArgTypes.length; ++i) {
                    if (!I18nConversionCategory.isSubsetOf(lhsArgTypes[i], rhsArgTypes[i])) {
                        return false;
                    }
                }
                return true;
            }

            if (AnnotationUtils.areSameIgnoringValues(lhs, I18NINVALIDFORMAT)
                    && AnnotationUtils.areSameIgnoringValues(rhs, I18NINVALIDFORMAT)) {
                return (AnnotationUtils.getElementValue(rhs, "value", String.class, true)).equals(AnnotationUtils
                        .getElementValue(lhs, "value", String.class, true));
            }

            if (AnnotationUtils.areSameIgnoringValues(lhs, I18NFORMAT)) {
                lhs = I18NFORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, I18NFORMAT)) {
                rhs = I18NFORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, I18NINVALIDFORMAT)) {
                lhs = I18NINVALIDFORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, I18NINVALIDFORMAT)) {
                rhs = I18NINVALIDFORMAT;
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, I18NFORMATFOR)) {
                lhs = I18NFORMATFOR;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, I18NFORMATFOR)) {
                rhs = I18NFORMATFOR;
            }

            return super.isSubtype(rhs, lhs);
        }
    }
}
