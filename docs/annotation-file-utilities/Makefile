
.PHONY: all test clean
all: annotation-file-format.pdf annotation-file-format.html

test: all

.PHONY: figures
figures:
	${MAKE} -C figures

export BIBINPUTS = $BIBINPUTS:.:../manual/plume-bib
# ../manual contains ifptex.sty, which lwarp needs.  The trailing ":" of an
# empty ${TEXINPUTS} makes TeX also search its default directories.
export TEXINPUTS := .:../manual:${TEXINPUTS}

annotation-file-format.pdf: plume-bib figures
	latexmk -bibtex -pdf annotation-file-format.tex

# lwarpmk requires configuration files that are written when LaTeX processes
# the source file in print mode, which creating the .pdf file does.
annotation-file-format.html: annotation-file-format.pdf manual.css favicon-checkerframework.png ../manual/lwarp-workarounds.tex ../manual/lwarp-postprocess ../manual/ifptex.sty
	lwarpmk html -p annotation-file-format
# lwarp writes the image list only if the document has images that lwarp
# renders from LaTeX, and `lwarpmk limages` fails if the list does not exist.
	if [ -f annotation-file-format-images.txt ] ; then lwarpmk limages -p annotation-file-format ; fi
	../manual/lwarp-postprocess annotation-file-format.html

# The HTML version uses the Checker Framework manual's style.  The file is
# copied, not referenced, because the website puts the two manuals in
# different directories.
manual.css: ../manual/manual.css
	cp -p $< $@

favicon-checkerframework.png: ../logo/Checkmark/CFCheckmark_favicon.png
	cp -p $< $@

plume-bib:
	../../checker/bin-devel/clone-or-update.sh https://github.com/mernst/plume-bib.git ../manual/plume-bib

.PHONY: clean
clean:
	@latexmk -silent -C annotation-file-format.tex || latexmk -C annotation-file-format.tex
	@\rm -f annotation-file-format_html.* *.lwarpmkconf lwarpmk.conf *.cut *.sidetoc lwarp.ist lwarp.xdy
	@\rm -f lwarp_mathjax.txt lwarp_one_limage.txt sample_project.css *-images.txt
	@\rm -f annotation-file-format.html manual.css favicon-checkerframework.png lwarp.css lwarp_formal.css lwarp_sagebrush.css
	@\rm -rf annotation-file-format-images
	@make -C figures --quiet clean
