# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

# To regenerate this list, run:   latex-process-inputs -makefilelist manual.tex
TEX_FILES = \
manual.tex \
manual-style.tex \
introduction.tex \
nullness-checker.tex \
map-key-checker.tex \
optional-checker.tex \
interning-checker.tex \
lock-checker.tex \
index-checker.tex \
fenum-checker.tex \
tainting-checker.tex \
regex-checker.tex \
formatter-checker.tex \
i18n-format-checker.tex \
propkey-checker.tex \
signature-checker.tex \
guieffect-checker.tex \
units-checker.tex \
signedness-checker.tex \
constant-value-checker.tex \
aliasing-checker.tex \
reflection-checker.tex \
reflection-inference-rules.tex \
subtyping-checker.tex \
external-checkers.tex \
typestate-checker.tex \
generics.tex \
advanced-features.tex \
warnings.tex \
inference.tex \
annotating-libraries.tex \
creating-a-checker.tex \
external-tools.tex \
faq.tex \
troubleshooting.tex

all: manual.pdf manual.html

.PHONY: figures-all
figures-all:
	${MAKE} -C figures all

manual.pdf: bib-update figures-all check-labels
	latexmk -silent -pdf -interaction=nonstopmode manual.tex || latexmk -gg -pdf -interaction=nonstopmode manual.tex

html: manual.html
manual.html: manual.pdf CFLogo.png favicon-checkerframework.png ../api
	hevea -fix -exec xxdate.exe manual.tex
# I'm not sure why this is necessary; "hevea -fix" should run it automatically.
# Also, you need ImageMagick 6.8.0-2 Beta or later to avoid a bug.
	imagen manual
	./add-favicon-to-manual
# The following three lines are only necessary when using Hevea before version 2.04.
# With version 2.04 they have no effect but do no harm.
	\mv -f manual.html manual.html-with-htoc
	./hevea-retarget-crossrefs < manual.html-with-htoc > manual.html
	\rm -f manual.html-with-htoc
# The below is not necessary since Hevea mkes good tables of contents itself.
#	perl -pi -e 's/(<!--TOC section Introduction-->)/$$1\n\n<p>Contents:<\/p>\n<!-- start toc.  do not edit; run html-update-toc instead -->\n<!-- end toc -->\n\n<br>\n/' manual.html
#	html-update-toc manual.html
# Add CSS styling, since \newstyle doesn't work for me.
	sed -i -e "s%<style type=\"text/css\">%<style type=\"text/css\">\nimg { max-width: 100\%; max-height: 100\%; }%" manual.html

../api:
	cd ../.. && ./gradlew allJavadoc

CFLogo.png: ../logo/Logo/CFLogo.png
	cp -p $< $@

favicon-checkerframework.png: ../logo/Checkmark/CFCheckmark_favicon.png
	cp -p $< $@

# Ensure that every section of the manual has a \label{} command.
.PHONY: check-labels
check-labels:
	if ( grep -n '^\\\(chapter\|\(sub\)*section\|paragraph\){' *.tex | grep -v "{.*\\label{.*}}" ) ; then false ; else true ; fi

export BIBINPUTS = $BIBINPUTS:.:bib
bib:
	git clone https://github.com/mernst/plume-bib.git bib
.PHONY: bib-update
bib-update: bib
# Even if this command fails, it does not terminate the make job.
# However, to skip it, invoke make as:  make NOGIT=1 ...
ifndef NOGIT
	-(cd bib && git pull && make)
endif

# Leaves manual.html, and .svg files that it references.
clean:
	@\rm -f *.aux *.blg *.dvi *.haux *.htoc *.idx *.ilg *.ind *.log *.out *.pdf *.ps *.toc manual.image.tex

very_clean: clean
	@\rm -f manual.html CFLogo.png

.PHONY: checklink
checklink:
	${CHECKLINK}/checklink -q -e `grep -v '^#' ${CHECKLINK}/checklink-args.txt` manual.html

.PHONY: tags
tags: TAGS
TAGS: ${TEX_FILES}
	etags ${TEX_FILES}
