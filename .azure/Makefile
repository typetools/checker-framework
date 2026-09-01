.PHONY: all test clean default
default: all
all: azure-pipelines-daily.yml

test: all

PLUME_SCRIPTS=../checker/bin-devel/.plume-scripts

%: %.m4 defs-common.m4 defs.m4 jobs.m4 ${PLUME_SCRIPTS}
	@chmod -f +w $@ || true
	m4 $< > $@
	${PLUME_SCRIPTS}/squeeze-blank-lines $@
# Don't do this for now, because it breaks prek's end-of-file-fixer.
#	@chmod -w $@

${PLUME_SCRIPTS}:
	cd .. && ./gradlew getPlumeScripts

clean:
	@rm -f azure-pipelines.yml azure-pipelines-daily.yml
