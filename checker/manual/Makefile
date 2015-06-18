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
interning-checker.tex \
lock-checker.tex \
fenum-checker.tex \
tainting-checker.tex \
regex-checker.tex \
formatter-checker.tex \
i18n-format-checker.tex \
propkey-checker.tex \
signature-checker.tex \
guieffect-checker.tex \
units-checker.tex \
constant-value-checker.tex \
aliasing-checker.tex \
linear-checker.tex \
igj-checker.tex \
javari-checker.tex \
reflection-checker.tex \
reflection-inference-rules.tex \
subtyping-checker.tex \
external-checkers.tex \
typestate-checker.tex \
generics.tex \
advanced-features.tex \
warnings.tex \
annotating-libraries.tex \
creating-a-checker.tex \
external-tools.tex \
faq.tex \
troubleshooting.tex

all: doc manual.pdf manual.html

.PHONY: figures-all
figures-all:
	${MAKE} -C figures all

doc:
	ln -sf ../doc doc

manual.pdf: bib-update figures-all
	pdflatex manual.tex
	bibtex manual
	pdflatex manual.tex
	while grep "Rerun to get" manual.log; do pdflatex manual.tex; done

html: manual.html
manual.html: manual.pdf
	hevea -fix -exec xxdate.exe manual.tex
	./add-favicon-to-manual
	cp logo/Logo/CFLogo.png CFLogo.png
# The following three lines are only necessary when using Hevea before version 2.04
# With version 2.04 they have no effect but do no harm.
	\mv -f manual.html manual.html-with-htoc
	./hevea-retarget-crossrefs < manual.html-with-htoc > manual.html
	\rm -f manual.html-with-htoc
# The below is not necessary since Hevea mkes good tables of contents itself.
#	perl -pi -e 's/(<!--TOC section Introduction-->)/$$1\n\n<p>Contents:<\/p>\n<!-- start toc.  do not edit; run html-update-toc instead -->\n<!-- end toc -->\n\n<br>\n/' manual.html
#	html-update-toc manual.html

export BIBINPUTS = $BIBINPUTS:.:bib
bib:
ifdef PLUMEBIB
	ln -s ${PLUMEBIB} bib
else
	git clone https://github.com/mernst/plume-bib.git bib
endif
.PHONY: bib-update
bib-update: bib
# Even if this command fails, it does not terminate the make job.
# However, to skip it, invoke make as:  make NOGIT=1 ...
ifndef NOGIT
	-(cd bib && git pull && make)
endif

# Leaves manual.html, and .svg files that it references.
clean:
	@\rm -f doc *.aux *.blg *.dvi *.haux *.htoc *.idx *.ilg *.ind *.log *.out *.pdf *.ps *.toc

very_clean: clean
	@\rm manual.html

.PHONY: checklink
checklink:
	${INV}/scripts/checklink -q -e `grep -v '^#' ${INV}/scripts/checklink-args.txt` manual.html

.PHONY: tags
tags: TAGS
TAGS: ${TEX_FILES}
	etags ${TEX_FILES}
