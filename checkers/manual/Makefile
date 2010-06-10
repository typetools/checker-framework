# To regenerate this list, run:   latex-process-inputs.pl -list manual.tex
TEX_FILES = manual.tex introduction.tex nullness-checker.tex interning-checker.tex igj-checker.tex javari-checker.tex lock-checker.tex tainting-checker.tex basic-checker.tex advanced-features.tex warnings.tex annotating-libraries.tex creating-a-checker.tex external-tools.tex faq.tex troubleshooting.tex

all: figures-all doc manual.pdf manual.html

figures-all:
	${MAKE} -C figures all

doc:
	ln -sf ../doc doc

manual.dvi: ${TEX_FILES}
	$(MAKE) -C figures
	latex manual.tex
	bibtex manual
	latex manual.tex
	latex manual.tex

manual.pdf: manual.dvi
	pdflatex manual.tex
	bibtex manual
	pdflatex manual.tex
	pdflatex manual.tex

manual.ps: manual.dvi
	dvips -P cmz -t letter -o manual.ps manual.dvi

html: manual.html
manual.html: manual.dvi
	hevea -fix -exec xxdate.exe urlhref.hva manual.tex
	hevea -exec xxdate.exe urlhref.hva manual.tex
	\mv -f manual.html manual.html-with-htoc
	retarget-hevea-crossrefs < manual.html-with-htoc > manual.html
	\rm -f manual.html-with-htoc
	perl -pi -e 's/\/&#XA0;/\/~/' manual.html
#	perl -pi -e 's/(<!--TOC section Introduction-->)/$$1\n\n<p>Contents:<\/p>\n<!-- start toc.  do not edit; run html-update-toc instead -->\n<!-- end toc -->\n\n<br>\n/' manual.html
#	html-update-toc manual.html

clean:
	@\rm -f doc *.aux *.blg *.haux *.htoc *.log *.pdf *.ps *.dvi *.idx *.ind *.ilg *.toc

very_clean: clean
	@\rm manual.html

.PHONY: checklink
checklink:
	${INV}/scripts/checklink -q -e `cat ${INV}/scripts/checklink-args.txt` manual.html

tags: TAGS
TAGS: ${TEX_FILES}
	etags ${TEX_FILES}
