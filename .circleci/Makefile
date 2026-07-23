.PHONY: all test clean default
default: all
all: config.yml
# TODO: all: config.yml config-daily.yml

test: all

PLUME_SCRIPTS=../checker/bin-devel/.plume-scripts

%: %.m4 ../.azure/defs-common.m4 defs.m4 ../.azure/jobs.m4 ${PLUME_SCRIPTS}
	@chmod -f +w $@ || true
	m4 $< > $@
	${PLUME_SCRIPTS}/squeeze-blank-lines $@
	@chmod -w $@

validate: config.yml
	cd .. && circleci config validate

${PLUME_SCRIPTS}:
	make -C .. get-plume-scripts

clean:
	@rm -f config.yml
