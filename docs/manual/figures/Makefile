# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

PDFFILES = $(patsubst %.svg,%.pdf,$(wildcard *.svg))

RSVG_CONVERT_VERSION := $(shell rsvg-convert --version 2>/dev/null)

all: ${PDFFILES} clean-obsolete-files svg-copy

svg-copy:
	-chmod -f +w ../*.svg
	cp -pf *.svg ..
	chmod -f -w ../*.svg

%.pdf : %.svg
ifdef RSVG_CONVERT_VERSION
	rsvg-convert -f pdf -o $@ $<
else
	convert $< $@
endif

clean: clean-obsolete-files
	@\rm -f ${PDFFILES}

clean-obsolete-files:
	@\rm -f *.png
	@\rm -f *.eps
