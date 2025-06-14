all default: \
 Dockerfile-ubuntu-jdkbase Dockerfile-ubunturolling-jdkbase \
 Dockerfile-ubuntu-jdk11 Dockerfile-ubuntu-jdk11-plus \
 Dockerfile-ubuntu-jdk17 Dockerfile-ubuntu-jdk17-plus \
 Dockerfile-ubuntu-jdk21 Dockerfile-ubuntu-jdk21-plus \
 Dockerfile-ubuntu-jdk24 Dockerfile-ubuntu-jdk24-plus

%: %.m4 Dockerfile-ubuntu-base-contents.txt
	m4 $< > $@
