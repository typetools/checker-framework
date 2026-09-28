
.PHONY: all test clean
all: annotation-file-format.pdf annotation-file-format.html

test: all

.PHONY: figures
figures:
	${MAKE} -C figures

export BIBINPUTS = $BIBINPUTS:.:../manual/plume-bib

annotation-file-format.pdf: plume-bib figures
	latexmk -bibtex -pdf annotation-file-format.tex

# lwarpmk requires configuration files that are written when LaTeX processes
# the source file in print mode, which creating the .pdf file does.
annotation-file-format.html: annotation-file-format.pdf
	lwarpmk html annotation-file-format
	lwarpmk limages annotation-file-format
	../manual/lwarp-postprocess annotation-file-format.html

plume-bib:
	../../checker/bin-devel/clone-or-update.sh https://github.com/mernst/plume-bib.git ../manual/plume-bib

.PHONY: clean
clean:
	@latexmk -silent -C annotation-file-format.tex || latexmk -C annotation-file-format.tex
	@\rm -f annotation-file-format_html.* *.lwarpmkconf lwarpmk.conf *.cut *.sidetoc lwarp.ist lwarp.xdy
	@\rm -f lwarp_mathjax.txt lwarp_one_limage.txt sample_project.css *-images.txt
	@\rm -f annotation-file-format.html lwarp.css lwarp_formal.css lwarp_sagebrush.css
	@\rm -rf annotation-file-format-images
	@make -C figures --quiet clean
