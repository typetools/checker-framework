all default: azure-pipelines.yml azure-pipelines-daily.yml

%: %.m4 defs.m4
	m4 $< > $@

clean:
	rm -f azure-pipelines.yml azure-pipelines-daily.yml
