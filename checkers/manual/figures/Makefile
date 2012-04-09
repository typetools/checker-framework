EPSFILES = fenum.eps igj.eps interning.eps javari.eps nullness.eps nullness-and-raw.eps regex.eps signature-types.eps
PDFFILES = fenum.pdf igj.pdf interning.pdf javari.pdf nullness.pdf nullness-and-raw.pdf regex.pdf signature-types.pdf
PNGFILES = fenum.png igj.png interning.png javari.png nullness.png nullness-and-raw.png regex.png signature-types.png

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
# Direct dia translation overlays the "" and vowel in "Fully" and "Field".
signature-types.png: signature-types.pdf
	convert -geometry 406x224 $< $@

clean:
	@\rm *.eps
	@\rm *.pdf
	@\rm *.png
