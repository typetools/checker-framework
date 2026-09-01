.PHONY: all test clean default validate
default: all
all: config.yml
# TODO: all: config.yml config-daily.yml

test: all

PLUME_SCRIPTS=../checker/bin-devel/.plume-scripts

%: %.m4 ../.azure/defs-common.m4 defs.m4 ../.azure/jobs.m4 ${PLUME_SCRIPTS}
	@chmod -f +w $@ || true
	m4 $< > $@
	${PLUME_SCRIPTS}/squeeze-blank-lines $@
# Don't do this for now, because it breaks prek's end-of-file-fixer.
#	@chmod -w $@

validate: config.yml
	cd .. && circleci config validate

${PLUME_SCRIPTS}:
	cd .. && ./gradlew getPlumeScripts

clean:
	@rm -f config.yml
