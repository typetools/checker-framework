all default: config.yml
# TODO: all default: config.yml config-daily.yml

PLUME_SCRIPTS=../checker/bin-devel/.plume-scripts

%: %.m4 ../.azure/defs-common.m4 defs.m4 ../.azure/jobs.m4 ${PLUME_SCRIPTS}
	@chmod -f +w $@ || true
	m4 $< > $@
	${PLUME_SCRIPTS}/squeeze-blank-lines $@
	@chmod -w $@

validate: config.yml
	cd .. && circleci config validate

${PLUME-SCRIPTS}:
	make -C .. get-plume-scripts

clean:
	rm -f config.yml
