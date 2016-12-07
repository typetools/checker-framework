package org.checkerframework.framework.util;

import com.sun.source.tree.CompilationUnitTree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Created by jburke on 6/12/14. TODO: Document this class. It is currently not used within the
 * framework.
 */
public class ComboLog {

    public static CompilationUnitTree root;

    private static class Agreement {
        public Agreement(
                final String checker,
                final String compilationUnit,
                final String type1Atm,
                final String type2Atm) {
            this.compilationUnit = compilationUnit;
            this.checker = checker;
            this.type1Atm = type1Atm;
            this.type2Atm = type2Atm;
        }

        public final String compilationUnit;
        public final String checker;
        public final String type1Atm;
        public final String type2Atm;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Agreement agreement = (Agreement) o;

            return compilationUnit.equals(agreement.compilationUnit)
                    && checker.equals(agreement.checker)
                    && type1Atm.equals(agreement.type1Atm)
                    && type2Atm.equals(agreement.type2Atm);
        }

        @Override
        public int hashCode() {
            int result = compilationUnit.hashCode();
            result = 31 * result + checker.hashCode();
            result = 31 * result + type1Atm.hashCode();
            result = 31 * result + type2Atm.hashCode();
            return result;
        }
    }

    private static Agreement parseAgreement(final String line) {
        final String[] tokens = line.split(del_r);
        if (tokens.length != 4) {
            throw new RuntimeException("Could not parseAgreement from: " + line);
        }

        return new Agreement(tokens[0], tokens[1], tokens[2], tokens[3]);
    }

    private static String del = "||";
    private static String del_r = "\\|\\|";
    private static File subtypeFile = new File("/Users/jburke/Documents/tmp/combo.txt");
    private static HashSet<Agreement> foundCombos;
    private static BufferedWriter comboWriter;

    private static String atmToClassStr(final AnnotatedTypeMirror atm) {
        return atm.getClass().getSimpleName().toString();
    }

    public static void writeCombo(
            AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs, String checker) {
        if (subtypeFile.length() > 100000) {
            return;
        }

        if (foundCombos == null) {
            initialize();
        }

        final Agreement agr =
                new Agreement(
                        root.getSourceFile().getName(),
                        checker.getClass().getSimpleName(),
                        atmToClassStr(rhs),
                        atmToClassStr(lhs));
        if (!foundCombos.contains(agr)) {
            foundCombos.add(agr);
            writeCombo(agr);
        }
    }

    private static void writeCombo(final Agreement agreement) {
        if (foundCombos == null) {
            initialize();
        }

        List<String> tokens =
                Arrays.asList(
                        agreement.compilationUnit,
                        agreement.checker,
                        agreement.type1Atm,
                        agreement.type2Atm);
        final String line = PluginUtil.join(del, tokens);
        try {
            comboWriter.write(line);
            comboWriter.newLine();
            comboWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(line, e);
        }
    }

    private static void initialize() {
        foundCombos = new HashSet<>();
        try {
            if (subtypeFile.exists()) {
                final BufferedReader reader = new BufferedReader(new FileReader(subtypeFile));

                String line;
                Agreement combo;
                do {
                    line = reader.readLine();
                    if (line != null) {
                        combo = parseAgreement(line);
                        foundCombos.add(combo);
                    }
                } while (line != null);

                reader.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Exception reading file " + subtypeFile.getAbsolutePath(), e);
        }

        try {
            comboWriter = new BufferedWriter(new FileWriter(subtypeFile, true));
        } catch (IOException e) {
            throw new RuntimeException("Exception opening subtype writer: " + subtypeFile, e);
        }
    }
}
