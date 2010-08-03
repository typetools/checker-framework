EPSFILES = igj.eps interning.eps javari.eps nullness.eps nullness-and-raw.eps
PDFFILES = igj.pdf interning.pdf javari.pdf nullness.pdf nullness-and-raw.pdf
PNGFILES = igj.png interning.png javari.png nullness.png nullness-and-raw.png

all: ${EPSFILES} ${PDFFILES} ${PNGFILES} png-copy

png-copy: ${PNGFILES}
	cp -pf ${PNGFILES} ..

%.eps : %.dia
	dia -t eps $<

%.pdf : %.eps
	epstopdf $<

%.png : %.dia
	dia -t png $<

# Direct dia translation overlays the "R" and "e" in "ReadOnly".
igj.png: igj.pdf
	convert -geometry 406x224 $< $@
javari.png: javari.pdf
	convert -geometry 406x224 $< $@

clean:
	@\rm *.eps
	@\rm *.pdf
	@\rm *.png
