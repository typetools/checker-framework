#!/usr/bin/python

# The google-java-format program (https://github.com/google/google-java-format)
# reformats Java source code, but it creates poor formatting for annotations
# in comments.
# Run this script on files after running google-java-format, and it will perform
# small changes in place to improve formatting of annotations in comments.
# If called with no arguments, it reads from and writes to standard output.
#
# You typically will not run this program directly; it is run by
# run-google-java-format.py.

from __future__ import print_function

import os
import os.path
import re
import sys

def eprint(*args, **kwargs):
    "Print to standard error"
    print(*args, file=sys.stderr, **kwargs)


# These are annotations that should not go on their own line.
# They are type annotations: their @Target annotation contains "TYPE_USE".
# To generate this list (last done on 2016-06-23):
#   ag --file-search-regex 'src/org/checkerframework|checker/examples/units-extension|framework/tests' --files-with-matches '^@Target\b.*TYPE_USE' $cf | sed 's/.*\///' | awk '{print $1} END {print "NotNull.java"; print "UB_TOP.java"; print "LB_TOP.java";}' | sed 's/\(.*\)\.java/    "\1",/' | sort | uniq
typeAnnotations = set([
    "A",
    "ACCBottom",
    "Acceleration",
    "ACCTop",
    "AlwaysSafe",
    "Angle",
    "AnnoWithStringArg",
    "Area",
    "ArrayLen",
    "AwtAlphaCompositingRule",
    "AwtColorSpace",
    "AwtCursorType",
    "AwtFlowLayout",
    "B",
    "BinaryName",
    "BinaryNameForNonArray",
    "BinaryNameForNonArrayInUnnamedPackage",
    "BinaryNameInUnnamedPackage",
    "BoolVal",
    "Bottom",
    "BottomQualifier",
    "BottomVal",
    "C",
    "CCBottom",
    "CCTop",
    "cd",
    "ClassBound",
    "ClassGetName",
    "ClassGetSimpleName",
    "ClassVal",
    "ClassValBottom",
    "CompilerMessageKey",
    "Constant",
    "Critical",
    "Current",
    "D",
    "DefaultType",
    "degrees",
    "Dependent",
    "DoubleVal",
    "E",
    "Encrypted",
    "EqualToLength",
    "Even",
    "F",
    "FBCBottom",
    "Fenum",
    "FenumBottom",
    "FenumTop",
    "FieldDescriptor",
    "FieldDescriptorForArray",
    "Format",
    "FormatBottom",
    "Frequency",
    "FullyQualifiedName",
    "g",
    "GTENegativeOne",
    "GuardedBy",
    "GuardedByBottom",
    "GuardedByUnknown",
    "GuardSatisfied",
    "h",
    "H1Bot",
    "H1Invalid",
    "H1Poly",
    "H1S1",
    "H1S2",
    "H1Top",
    "H2Bot",
    "H2Poly",
    "H2S1",
    "H2S2",
    "H2Top",
    "Hz",
    "I18nFormat",
    "I18nFormatBottom",
    "I18nFormatFor",
    "I18nInvalidFormat",
    "I18nUnknownFormat",
    "Identifier",
    "IdentifierOrArray",
    "ImplicitAnno",
    "Initialized",
    "InternalForm",
    "Interned",
    "IntVal",
    "InvalidFormat",
    "K",
    "KeyFor",
    "KeyForBottom",
    "KeyForType",
    "kg",
    "kHz",
    "km",
    "km2",
    "kmPERh",
    "LazyNonNull",
    "LB_TOP",
    "LeakedToResult",
    "Length",
    "Linear",
    "LocalizableKey",
    "Localized",
    "LowerBoundUnknown",
    "LTEqLengthOf",
    "LTLengthOf",
    "Luminance",
    "m",
    "m2",
    "Mass",
    "MaybeAliased",
    "MethodDescriptor",
    "MethodVal",
    "MethodValBottom",
    "min",
    "MinLen",
    "mm",
    "mm2",
    "mol",
    "MonotonicNonNull",
    "MonotonicNonNullType",
    "MonotonicOdd",
    "mPERs",
    "mPERs2",
    "MultiPolyRegex",
    "MultiPolyTainted",
    "MultiRegex",
    "MultiTainted",
    "MultiUntainted",
    "MultiVar",
    "MultiWild",
    "NonLeaked",
    "NonNegative",
    "NonNull",
    "NonNullType",
    "NonRaw",
    "Normal",
    "NotNull",
    "NotQualifier",
    "Nullable",
    "NullableType",
    "Odd",
    "Parent",
    "PatternA",
    "PatternAB",
    "PatternAC",
    "PatternB",
    "PatternBC",
    "PatternBottomFull",
    "PatternBottomPartial",
    "PatternC",
    "PatternUnknown",
    "PolyAll",
    "PolyEncrypted",
    "PolyGuardedBy",
    "PolyInterned",
    "PolyKeyFor",
    "PolyNull",
    "PolyNullType",
    "PolyRaw",
    "PolyReflection",
    "PolyRegex",
    "PolySignature",
    "PolyTainted",
    "PolyUI",
    "PolyUnit",
    "Positive",
    "PropertyKey",
    "PropertyKeyBottom",
    "PurityUnqualified",
    "Qualifier",
    "radians",
    "Raw",
    "ReflectBottom",
    "Regex",
    "RegexBottom",
    "ReportUnqualified",
    "s",
    "Sibling1",
    "Sibling2",
    "SiblingWithFields",
    "SignatureBottom",
    "Signed",
    "SourceNameForNonArrayNonInner",
    "SourceNameForNonInner",
    "Speed",
    "StringVal",
    "SubQual",
    "Substance",
    "SuperQual",
    "SwingBoxOrientation",
    "SwingCompassDirection",
    "SwingElementOrientation",
    "SwingHorizontalOrientation",
    "SwingSplitPaneOrientation",
    "SwingTextOrientation",
    "SwingTitleJustification",
    "SwingTitlePosition",
    "SwingVerticalOrientation",
    "Tainted",
    "Temperature",
    "Time",
    "Top",
    "UB_TOP",
    "UI",
    "UnderInitialization",
    "Unique",
    "UnitsBottom",
    "UnknownClass",
    "UnknownInitialization",
    "UnknownInterned",
    "UnknownKeyFor",
    "UnknownLocalized",
    "UnknownMethod",
    "UnknownPropertyKey",
    "UnknownRegex",
    "UnknownSignedness",
    "UnknownUnits",
    "UnknownVal",
    "Unsigned",
    "UnsignednessBottom",
    "Untainted",
    "Unusable",
    "UpperBoundUnknown",
    "Value",
    "Var",
    "WholeProgramInferenceBottom",
    "Wild",
])

# File .type-annotations can add to the typeAnnotations variable.
if os.path.isfile(".type-annotations"):
    execfile('execfile_example.py')

debug = False
# debug = True

# Two annotations in a row, or an annotation abutting array brackets "[]".
# Space is inserted between.
abuttingannoRegex = re.compile(r"(/\*@[A-Za-z0-9_]+\*/)(\[\]|/\*@[A-Za-z0-9_]+\*/)")
# Voodoo annotation with extra space after
voodootrailingspaceRegex = re.compile(r"(/\*>>> ?@.*\bthis\*/) (\))")

# Matches, at the end of its line (in capturing group 2):
#  * An annotation
#    This is a bit dangerous!  It might be within a comment.
#    The script tries to heuristically detect this.
#  * An annotation in comments
#  * a comment like /*offset = */ that should appear right
#    before the argument it documents.
# The annotation will be moved to the beginning of the following line,
# if it appears in typeAnnotations.
# The regex does not permit "()" within a string in an annotation, such as
#   @GuardedBy("c1.getFieldPure2()")
trailingannoRegex = re.compile(r"^(.*?)[ \t]*(@[A-Za-z0-9_.]+( *\([^)]*\))?|/\*@[A-Za-z0-9_.]+( *\([^)]*\))?\*/|/\* *[A-Za-z0-9_]+ *= *\*/)$")

whitespaceRegex = re.compile(r"^([ \t]*).*$")

emptylineRegex = re.compile(r"^[ \t]*$")

# Heuristic: matches if the line might be within a //, /*, or Javadoc comment.
withinCommentRegex = re.compile(r"//|/\*(?!.*\/*/)|^[ \t]*\*[ \t]")

startsWithCommentRegex = re.compile(r"^[ \t]*(//|/\*$|/\*[^@]|\*|void\b)")

def insert_after_whitespace(insertion, s):
    """Return s, with insertion inserted after its leading whitespace."""
    m = re.match(whitespaceRegex, s)
    return s[0:m.end(1)] + insertion + s[m.end(1):]


def fixup_loop(infile, outfile):
    """Fix up formatting while reading from infile and writing to outfile."""
    prev = ""           # previous line, which might end with a type annotation.
    for line in infile:
        # Handle trailing space after a voodoo comment
        line = voodootrailingspaceRegex.sub(r"\1\2", line)
        # Handle abutting annotations in comments
        m = re.search(abuttingannoRegex, line)
        while m:
            if debug: print("found abutting", line)
            line = line[0:m.end(1)] + " " + line[m.start(2):]
            m = re.search(abuttingannoRegex, line)
        # Don't move an annotation to the start of a comment line
        if (re.search(startsWithCommentRegex, line)):
            m = None
            if debug: print("Don't prepend to comment", prev, line)
        else:
            # Handle annotations at end of line that should be at beginning of
            # next line.
            m = re.search(trailingannoRegex, prev)
            if debug: print("trailing? (pre-loop)", m, prev, line)
        while m:
            if debug: print("found trailing", prev, line)
            anno = m.group(2)
            if not base_annotation(anno) in typeAnnotations:
                break
            if debug: print("prev was:", prev)
            candidate_prev = prev[0:m.end(1)] + prev[m.end(2):]
            if debug: print("candidate_prev is :", candidate_prev)
            if re.search(withinCommentRegex, candidate_prev):
                if debug: print("withinCommentRegex prohibits action")
                break
            prev = candidate_prev
            if debug: print("prev is:", prev)
            if re.search(emptylineRegex, prev):
                prev = ""
                if debug: print("prev is empty")
            if debug: print("line was:", line)
            line = insert_after_whitespace(anno + " ", line)
            if debug: print("line is :", line)
            m = re.search(trailingannoRegex, prev)
            if debug: print("trailing? (post-loop-body)", m, prev, line)
        outfile.write(prev)
        prev = line
    outfile.write(prev)

def base_annotation(annotation):
    """Remove leading and trailing comment characters, spaces, arguments, and at sign.
Example: base_annotation('/*@RequiresNonNull("FileIO.data_trace_state")*/' => 'RequiresNonNull'"""
    if debug: print("base_annotation <=", annotation)

    # Remove comments
    if annotation.startswith("/*"):
        annotation = annotation[2:]
    if annotation.endswith("*/"):
        annotation = annotation[:-2]

    # Remove arguments
    idx = annotation.find('(')
    if idx != -1:
        annotation = annotation[0:idx]

    # Remove package names
    idx = annotation.rfind('.')
    if idx != -1:
        annotation = annotation[idx+1:]

    annotation = annotation.strip()
    if annotation.startswith("@"):
        annotation = annotation[1:]
    if debug: print("base_annotation =>", annotation)
    return annotation


if len(sys.argv) == 1:
    fixup_loop(sys.stdin, sys.stdout)
else:
    for fname in sys.argv[1:]:
        outfname = fname + '.out'
        with open(fname,'r') as infile:
            with open(outfname ,'w') as outfile:
                fixup_loop(infile, outfile)
        os.rename(outfname, fname)
