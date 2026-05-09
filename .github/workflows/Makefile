all default: ci.yml

PLUME_SCRIPTS=../../checker/bin-devel/.plume-scripts

%: %.m4 ../../.azure/defs-common.m4 defs.m4 ../../.azure/jobs.m4 ${PLUME_SCRIPTS}
	@chmod -f +w $@ || true
	m4 $< > $@
	${PLUME_SCRIPTS}/squeeze-blank-lines $@
	@chmod -w $@

${PLUME-SCRIPTS}:
	make -C ../ee get-plume-scripts

clean:
	rm -f ci.yml
