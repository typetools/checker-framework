all default: \
 Dockerfile-ubuntu-jdkbase Dockerfile-ubunturolling-jdkbase \
 Dockerfile-ubuntu-jdkplus Dockerfile-ubunturolling-jdkplus \
 Dockerfile-ubuntu-jdk11 Dockerfile-ubuntu-jdk11-plus \
 Dockerfile-ubuntu-jdk17 Dockerfile-ubuntu-jdk17-plus \
 Dockerfile-ubuntu-jdk21 Dockerfile-ubuntu-jdk21-plus \
 Dockerfile-ubuntu-jdk25 Dockerfile-ubuntu-jdk25-plus

%: %.m4 Dockerfile-contents-ubuntu-base.m4 Dockerfile-contents-ubuntu-plus.m4 Dockerfile-contents-apt-clean.m4
	@chmod -f +w $@ || true
	m4 $< > $@
	@chmod -w $@
