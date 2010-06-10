
.SILENT:
.PHONEY: all clean

MAKE ?= make

RTJAR ?= $(JAVA_HOME)/jre/lib/rt.jar

folders = java/lang java/util java/io javax/annotation javax/lang

all: clean
	$(MAKE) annotated
	cd nullness; $(MAKE)
	cd igj; $(MAKE)
	cd javari; $(MAKE)
	$(MAKE) jdk.jar

clean:
	rm -rf annotated jdk.jar

annotated:
	mkdir annotated
	cd annotated; unzip	$(RTJAR) $(folders) > /dev/null

jdk.jar:
	cd annotated; jar cf ../jdk.jar .

showvars:
	pwd
	echo BUILD_CLASSES: ${BUILD_CLASSES}
	echo JAIFS: ${JAIFS}
	echo ANNOTATED: ${ANNOTATED}
