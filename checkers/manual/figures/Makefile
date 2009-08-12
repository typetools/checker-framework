all: pdf eps
pdf: igj.pdf interning.pdf javari.pdf nullness.pdf
eps: igj.eps interning.eps javari.eps nullness.eps

%.pdf : %.eps
	epstopdf $<

%.eps : %.dia
	dia -t eps $<

clean:
	@\rm *.eps
	@\rm *.pdf
