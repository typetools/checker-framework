all default: azure-pipelines.yml azure-pipelines-daily.yml

%: %.m4 defs-common.m4 defs.m4 jobs.m4
	@chmod -f +w $@ || true
	m4 $< > $@
# Squeeze multiple blank lines into one.
	cat -s $@ > $@.tmp
# Remove leading blank lines.
	sed '/[^[:blank:]]/,$$!d' $@.tmp > $@
# Remove trailing blank lines.
	sed -e :a -e '/^\s*$$/{$$d;N;ba' -e '}' $@ > $@.tmp
	mv -f $@.tmp $@
	@chmod -w $@

clean:
	rm -f azure-pipelines.yml azure-pipelines-daily.yml
