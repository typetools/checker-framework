#!/usr/bin/python

# The google-java-format project reformats Java source code to comply with
# Google Java Style.  It creates poor formatting for annotations in
# comments.  Run this script on files after running google-java-format, and
# it will perform small changes in place to improve formatting of
# annotations in comments.

import os
import re
import sys

# TODO: handle annotations with arguments

# TODO: complete this list
declarationAnnotations = {
    "/*@Deterministic*/",
    "/*@FormatMethod*/",
    "/*@Pure*/",
    "/*@SideEffectFree*/",
}

debug = False

abuttingannoRegex = re.compile(r"(/\*@[A-Za-z0-9_]+\*/)(\[\]|/\*@[A-Za-z0-9_]+\*/)")
trailingannoRegex = re.compile(r"^(.*?)[ \t]*(/\*@[A-Za-z0-9_]+\*/)$")
whitespaceRegex = re.compile(r"^([ \t]*).*$")
emptylineRegex = re.compile(r"^[ \t]*$")

def insert_after_whitespace(insertion, s):
    """Return s, with insertion inserted after its leading whitespace."""
    m = re.match(whitespaceRegex, s)
    return s[0:m.end(1)] + insertion + s[m.end(1):]


for fname in sys.argv[1:]:
    outfname = fname + '.out'

    with open(fname,'r') as infile:
        with open(outfname ,'w') as outfile:
            prev = ""           # previous line
            for line in infile:
                m = re.search(abuttingannoRegex, line)
                while m:
                    if debug: print "found abutting", line
                    line = line[0:m.end(1)] + " " + line[m.start(2):]
                    m = re.search(abuttingannoRegex, line)
                m = re.search(trailingannoRegex, prev)
                while m:
                    anno = m.group(2)
                    if anno in declarationAnnotations:
                        break
                    if debug: print "prev was:", prev
                    prev = prev[0:m.end(1)] + prev[m.end(2):]
                    if debug: print "prev is :", prev
                    if re.search(emptylineRegex, prev):
                        prev = ""
                        if debug: print "prev is':", prev
                    if debug: print "line was:", line
                    line = insert_after_whitespace(m.group(2) + " ", line)
                    if debug: print "line is :", line
                    m = re.search(trailingannoRegex, prev)
                outfile.write(prev)
                prev = line
            outfile.write(prev)

    os.rename(outfname, fname)
