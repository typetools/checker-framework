# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

.PHONY: all test clean
all: manual.pdf html ../api
test: all

.PHONY: figures-all
figures-all:
	${MAKE} -C figures all

# It is not harmful to run `latexmk` needlessly because it does its own dependency check,
# which depends on file contents, not timestamp.
manual.pdf: *.tex plume-bib figures-all check-labels
	latexmk -silent -pdf -interaction=nonstopmode manual.tex || latexmk -gg -pdf -interaction=nonstopmode manual.tex

# The HTML versions of the manual are produced by lwarp.
# manual.html is a single page; manual-multipage.html is the first of many pages.
.PHONY: html
html: manual.html manual-multipage.html

# lwarpmk requires configuration files that are written when LaTeX processes
# the source file in print mode.  Creating manual.pdf does so for manual.tex.
manual.html: manual.pdf manual.css lwarp-postprocess CFLogo.png favicon-checkerframework.png
	lwarpmk html manual
	lwarpmk limages manual
	./lwarp-postprocess manual.html

# The HTML files of the multi-page version of the manual, other than lwarp's
# intermediate files such as manual-multipage_html.html.
MULTIPAGE_HTML_FILES = ls manual-*.html | grep -v '_html\.html$$'

# The order-only prerequisite on manual.html prevents the two lwarpmk
# invocations from running in parallel, because they write the same files.
# The rule removes existing pages, so that no page remains from a
# previous build in which a chapter had a different name.
manual-multipage.html: manual.pdf manual-multipage.tex manual.css lwarp-postprocess CFLogo.png favicon-checkerframework.png | manual.html
	pdflatex -interaction=nonstopmode manual-multipage.tex
	\rm -f manual-*.html
	lwarpmk html manual-multipage
	lwarpmk limages manual-multipage
	./lwarp-postprocess $$(${MULTIPAGE_HTML_FILES})

../../checker/bin-devel/.git-scripts:
	../../checker/bin-devel/clone-or-update.sh https://github.com/plume-lib/git-scripts.git ../../checker/bin-devel/.git-scripts

../../checker/bin-devel/.plume-scripts:
	../../checker/bin-devel/clone-plume-scripts.sh

# Because `contributors.tex` is .PHONY, any target that depends on it is always remade.
.PHONY: contributors.tex
contributors.tex:
# Update git-scripts even if it is already cloned.
	../../checker/bin-devel/clone-or-update.sh https://github.com/plume-lib/git-scripts.git ../../checker/bin-devel/.git-scripts
	../../checker/bin-devel/.git-scripts/git-authors --latex --punctuation > $@

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
	../../checker/bin-devel/clone-or-update.sh https://github.com/mernst/plume-bib.git plume-bib


# Leaves the HTML files and the files that they reference.
.PHONY: clean
clean:
	@\rm -f *.aux *.blg *.dvi *.fdb_latexmk *.fls *.idx *.ilg *.ind *.log *.out *.pdf *.ps *.toc TAGS
	@\rm -f *_html.* *.lwarpmkconf lwarpmk.conf *.cut *.sidetoc lwarp.ist lwarp.xdy
	@\rm -f lwarp_mathjax.txt lwarp_one_limage.txt sample_project.css *-images.txt

.PHONY: very-clean very_clean
very-clean: very_clean
very_clean: clean
	@\rm -f manual.html manual-*.html manual.bbl CFLogo.png favicon-checkerframework.png
	@\rm -f lwarp.css lwarp_formal.css lwarp_sagebrush.css
	@\rm -rf manual-images manual-multipage-images

.PHONY: checklink
checklink:
	checklink-via-http

.PHONY: checklink-via-file
checklink-via-file: html
	${CHECKLINK}/checklink -q -e $$(grep -v '^#' ${CHECKLINK}/checklink-args.txt) manual.html $$(${MULTIPAGE_HTML_FILES})

# Example invocation:
#   CHECKLINK=$HOME/bin/src/checklink make checklink-via-http
.PHONY: checklink-via-http
checklink-via-http: html
	rm -rf ${HOME}/public_html/tmp-cf-manual
	(cd .. && cp -prf manual ${HOME}/public_html/tmp-cf-manual)
	${CHECKLINK}/checklink -q -e `grep -v '^#' ${CHECKLINK}/checklink-args.txt` $$( (echo manual.html; ${MULTIPAGE_HTML_FILES}) | sed 's%^%https://homes.cs.washington.edu/~mernst/tmp-cf-manual/%')

.PHONY: tags TAGS
TAGS: tags
# This target always runs because contributors.tex is always updated.
tags: *.tex ../../checker/bin-devel/.plume-scripts
# Update plume-scripts even if it is already cloned.
	../../checker/bin-devel/clone-plume-scripts.sh
# This is better than using "*.tex" because this command puts the files in the appropriate order.
	etags $(shell ../../checker/bin-devel/.plume-scripts/latex-process-inputs -list manual.tex)
