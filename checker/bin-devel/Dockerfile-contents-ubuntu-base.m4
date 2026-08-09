LABEL org.opencontainers.image.authors="Michael Ernst <mernst@cs.washington.edu>"

# According to
# https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/:
#  * Put "apt update" and "apt install" and "apt cleanup" in the same RUN command.
#  * Do not run "apt upgrade"; instead get upstream to update.

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install locales \
&& rm -rf /var/lib/apt/lists/* \
&& locale-gen "en_US.UTF-8"
ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8

# Always install JDK 21 to compile the code, even if tests run under a different JDK.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  openjdk-21-jdk
ENV JAVA21_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Known good combinations of JTReg and the JDK appear at https://builds.shipilev.net/jtreg/ .

# Dependencies for building and running the Checker Framework.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  ant \
  cpp \
  git \
  graphviz \
  jq \
  jtreg7 \
  libcurl3-gnutls \
  make \
  python3-requests \
  python3-setuptools \
  unzip \
  wget

define(`maven_version', `4.0.0-rc-6')dnl
# The checksum is the .sha512 file beside the tarball in the URL below.
# Update it whenever the Maven version defined above changes.
RUN export DEBIAN_FRONTEND=noninteractive \
&& cd /opt \
&& wget https://archive.apache.org/dist/maven/maven-4/maven_version/binaries/apache-maven-maven_version-bin.tar.gz \
&& echo "3fba58e1c345a5aa1dbacfa7aceaf7b1a0fa9626e368eec4814fa7a7ebf0fe74f0e41481faef77f95d8738f9c1365f918c8b8c94d7c28656f067db61a8af7f2e  apache-maven-maven_version-bin.tar.gz" | sha512sum --check --strict \
&& tar xzf apache-maven-maven_version-bin.tar.gz \
&& rm apache-maven-maven_version-bin.tar.gz
ENV PATH="/opt/apache-maven-maven_version/bin:$PATH"
