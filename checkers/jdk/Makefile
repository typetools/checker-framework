
.SILENT:
.PHONEY: all clean

MAKE ?= make
dirs = nullness igj javari
jdks = $(patsubst %,%/jdk.jar, $(dirs))

all: clean $(jdks)

clean:
	rm -f $(jdks)

%.jar:
	cd $(dir $@); $(MAKE) all
