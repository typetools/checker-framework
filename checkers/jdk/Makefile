
.SILENT:
.PHONEY: all clean

MAKE ?= make
dirs = nullness igj
jdks = $(patsubst %,%/jdk.jar, $(dirs))

all: clean $(jdks)

clean:
	rm -rf $(jdks)

%.jar:
	cd $(dir $@); $(MAKE) all
