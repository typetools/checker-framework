# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

PDFFILES = fenum.pdf igj.pdf initialization.pdf interning.pdf javari.pdf nullness.pdf nullness-and-rawness.pdf rawness.pdf regex.pdf signature-types.pdf aliasing.pdf formatter-hierarchy.pdf formatter-categories.pdf value-subtyping.pdf

all: ${PDFFILES} clean-obsolete-files svg-copy

svg-copy:
	cp -pf *.svg ..

%.pdf : %.svg
	rsvg-convert -f pdf -o $@ $<

clean: clean-obsolete-files
	@\rm -f ${PDFFILES}

clean-obsolete-files:
	@\rm -f *.png
	@\rm -f *.eps

