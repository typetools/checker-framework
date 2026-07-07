all default: azure-pipelines.yml azure-pipelines-daily.yml

PLUME_SCRIPTS=../checker/bin-devel/.plume-scripts

%: %.m4 defs-common.m4 defs.m4 jobs.m4 ${PLUME_SCRIPTS}
	@chmod -f +w $@ || true
	m4 $< > $@
	${PLUME_SCRIPTS}/squeeze-blank-lines $@
	@chmod -w $@

${PLUME_SCRIPTS}:
	make -C .. get-plume-scripts

clean:
	rm -f azure-pipelines.yml azure-pipelines-daily.yml
