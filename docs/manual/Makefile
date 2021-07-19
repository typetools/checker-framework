# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

# To regenerate this list, run:
# ../../checker/bin-devel/.plume-scripts/latex-process-inputs -makefilelist manual.tex
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
called-methods-checker.tex \
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
purity-checker.tex \
constant-value-checker.tex \
returns-receiver-checker.tex \
reflection-checker.tex \
reflection-inference-rules.tex \
initialized-fields-checker.tex \
aliasing-checker.tex \
subtyping-checker.tex \
external-checkers.tex \
typestate-checker.tex \
generics.tex \
advanced-features.tex \
warnings.tex \
inference.tex \
annotating-libraries.tex \
creating-a-checker.tex \
accumulation-checker.tex \
external-tools.tex \
faq.tex \
troubleshooting.tex \
contributors.tex

all: manual.pdf manual.html

.PHONY: figures-all
figures-all:
	${MAKE} -C figures all

manual.pdf: ${TEX_FILES} plume-bib-update figures-all check-labels
	latexmk -silent -pdf -interaction=nonstopmode manual.tex || latexmk -gg -pdf -interaction=nonstopmode manual.tex

html: manual.html
manual.html: manual.pdf CFLogo.png favicon-checkerframework.png ../api
	hevea -fix svg.hva -exec xxdate.exe manual.tex
# I'm not sure why this is necessary; "hevea -fix" should run it automatically.
# Also, you need ImageMagick 6.8.0-2 Beta or later to avoid a bug.
	imagen manual
	./add-favicon-to-manual
# The following three lines are only necessary when using Hevea before version 2.04.
# With version 2.04 they have no effect but do no harm.
	\mv -f manual.html manual.html-with-htoc
	./hevea-retarget-crossrefs < manual.html-with-htoc > manual.html
	\rm -f manual.html-with-htoc
# Add CSS styling, since \newstyle doesn't work for me.
	sed -i -e "s%<style type=\"text/css\">%<style type=\"text/css\">\nimg { max-width: 100\%; max-height: 100\%; }%" manual.html
# Add CSS styling for some links, since \ahrefloc doesn't permit styling
	sed -i -e 's%\(<a href="#[^"]*"\)\(><span style="font-size:small">&\#X1F517;</span></a>\)%\1 style="color:inherit; text-decoration:none"\2%g' manual.html

../../checker/bin-devel/.plume-scripts:
	cd ../.. && ./gradlew getPlumeScripts -q

.PHONY: contributors.tex
contributors.tex:
# Update plume-scripts even if it is already cloned
	cd ../.. && ./gradlew getPlumeScripts -q
	../../checker/bin-devel/.plume-scripts/git-authors --latex --punctuation > contributors.tex

../api:
	cd ../.. && ./gradlew allJavadoc

CFLogo.png: ../logo/Logo/CFLogo.png
	cp -p $< $@

favicon-checkerframework.png: ../logo/Checkmark/CFCheckmark_favicon.png
	cp -p $< $@

# Don't use \section; use \sectionAndLabel instead
.PHONY: check-labels
check-labels:
	if ( grep -n '^\\\(chapter\|\(sub\)*section\|paragraph\){' *.tex ) ; then false ; else true ; fi

export BIBINPUTS = $BIBINPUTS:.:plume-bib
plume-bib:
	(git clone --depth 1 -q https://github.com/mernst/plume-bib.git || git clone --depth 1 -q https://github.com/mernst/plume-bib.git)
.PHONY: plume-bib-update
plume-bib-update: plume-bib
# Even if this command fails, it does not terminate the make job.
# However, to skip it, invoke make as:  make NOGIT=1 ...
ifndef NOGIT
	-(cd plume-bib && git pull && make)
endif

# Leaves manual.html, and .svg files that it references.
clean:
	@\rm -f *.aux *.blg *.dvi *.haux *.htoc *.idx *.ilg *.ind *.log *.out *.pdf *.ps *.toc manual.image.tex

very_clean: clean
	@\rm -f manual.html CFLogo.png

.PHONY: checklink
checklink:
	checklink-via-http

.PHONY: checklink-via-file
checklink-via-file:
	${CHECKLINK}/checklink -q -e `grep -v '^#' ${CHECKLINK}/checklink-args.txt` manual.html

# Example invocation:
#   CHECKLINK=$HOME/bin/src/checklink make checklink-via-http
.PHONY: checklink-via-http
checklink-via-http: manual.html
	rm -rf ${HOME}/public_html/tmp-cf-manual
	(cd .. && cp -prf manual ${HOME}/public_html/tmp-cf-manual)
	${CHECKLINK}/checklink -q -e `grep -v '^#' ${CHECKLINK}/checklink-args.txt` https://homes.cs.washington.edu/~mernst/tmp-cf-manual/manual.html

.PHONY: tags
tags: TAGS
TAGS: ${TEX_FILES}
	etags ${TEX_FILES}
