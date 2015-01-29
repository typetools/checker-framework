# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

DIA?=dia

EPSFILES = fenum.eps igj.eps initialization.eps interning.eps javari.eps nullness.eps nullness-and-rawness.eps rawness.eps regex.eps signature-types.eps aliasing.eps formatter-hierarchy.eps formatter-categories.eps
PDFFILES = fenum.pdf igj.pdf initialization.pdf interning.pdf javari.pdf nullness.pdf nullness-and-rawness.pdf rawness.pdf regex.pdf signature-types.pdf aliasing.pdf formatter-hierarchy.pdf formatter-categories.pdf value-subtyping.pdf

all: ${EPSFILES} ${PDFFILES} png-clean svg-copy

png-clean:
	rm -f *.png

svg-copy:
	cp -pf *.svg ..

%.eps : %.svg
	$(DIA) -t eps $<

%.pdf : %.eps
	epstopdf $<

value-subtyping.eps:	

clean:
	@\rm ${EPSFILES}
	@\rm ${PDFFILES}
	rm -f *.png
