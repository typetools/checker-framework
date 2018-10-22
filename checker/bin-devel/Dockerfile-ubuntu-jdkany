# Create a Docker image that is ready to run the full Checker Framework tests,
# including building the manual and Javadoc.
# But it's used primarily for running miscellaneous tests such as the manual
# and Javadoc.

FROM ubuntu
MAINTAINER Michael Ernst <mernst@cs.washington.edu>

## Keep this file in sync with ../../docs/manual/troubleshooting.tex

# According to
# https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/:
#  * Put "apt-get update" and "apt-get install" in the same RUN command.
#  * Do not run "apt-get upgrade"; instead get upstream to update.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt-get -qqy update \
&& apt-get -qqy install \
  software-properties-common \
&& add-apt-repository -y ppa:webupd8team/java \
&& apt-get -qqy update \
&& echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections \
&& echo debconf shared/accepted-oracle-license-v1-1 seen true | /usr/bin/debconf-set-selections \
&& echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections \
&& apt-get -qqy install oracle-java8-installer \
&& apt-get -qqy install oracle-java8-set-default \
&& apt-get -qqy install \
  ant \
  cpp \
  git \
  gradle \
  libcurl3-gnutls \
  make \
  maven \
  mercurial \
  unzip \
  wget \
  default-jdk \
&& apt-get -qqy install \
  dia \
  hevea \
  imagemagick \
  latexmk \
  librsvg2-bin \
  maven \
  python-pip \
  texlive-font-utils \
  texlive-fonts-recommended \
  texlive-latex-base \
  texlive-latex-extra \
  texlive-latex-recommended \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/* \
&& pip install html5validator
