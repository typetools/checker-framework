all default: config.yml
# TODO: all default: config.yml config-daily.yml

%: %.m4 ../.azure/defs-common.m4 defs.m4 ../.azure/jobs.m4
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

validate: config.yml
	cd .. && circleci config validate

clean:
	rm -f config.yml
