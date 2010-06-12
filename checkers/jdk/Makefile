
MAKE ?= make
RTJAR ?= $(JAVA_HOME)/jre/lib/rt.jar

CHECKERS = nullness igj javari

all: clean classes
	$(MAKE) unmodified
	$(MAKE) jaifs annotated
	$(MAKE) jdk.jar

jdk.jar:
	cd annotated; jar cf ../jdk.jar .

# Helper

MODIFIED = $(shell find $(CHECKERS) -name '*.class')
UNZIP = \
	$(patsubst nullness/build/%.class,annotated/%.class, \
	  $(patsubst igj/build/%.class,annotated/%.class, \
	    $(patsubst javari/build/%.class,annotated/%.class, \
		  $(MODIFIED))))

unmodified: $(UNZIP)

# Delegates
clean: $(addsuffix .clean, $(CHECKERS))
	rm -rf jdk.jar annotated

classes: $(addsuffix .classes, $(CHECKERS))

jaifs: $(addsuffix .jaifs, $(CHECKERS))

annotated: $(addsuffix .annotated, $(CHECKERS))

annotated/%.class:
	mkdir -p annotated
	cd annotated; unzip $(RTJAR) '$*.class' > /dev/null

# Helper targets to invoke subdir make files
%.clean:
	cd $*; $(MAKE) clean

%.classes:
	cd $*; $(MAKE) classes

%.jaifs:
	cd $*; $(MAKE) jaifs

%.annotated:
	cd $*; $(MAKE) annotated
