# Create a Docker image that is ready to run the full Checker Framework tests,
# including building the manual and Javadoc, using JDK 11.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu
include(`Dockerfile-ubuntu-base-contents.txt')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  openjdk-11-jdk \
&& update-java-alternatives -s java-1.11.0-openjdk-amd64

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  asciidoctor \
  autoconf \
  devscripts \
  dia \
  hevea \
  imagemagick \
  junit \
  latexmk \
  librsvg2-bin \
  libasound2-dev libcups2-dev libfontconfig1-dev \
  libx11-dev libxext-dev libxrender-dev libxrandr-dev libxtst-dev libxt-dev \
  pdf2svg \
  rsync \
  shellcheck \
  shfmt \
  texlive-font-utils \
  texlive-fonts-recommended \
  texlive-latex-base \
  texlive-latex-extra \
  texlive-latex-recommended

# `pipx ensurepath` only adds to the path in newly-started shells.
# BUT, setting the path for the current user is not enough.
# Azure creates a new user and runs jobs as it.
# So, install into /usr/local/bin which is already on every user's path.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  pipx \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install black \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install flake8 \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install html5validator \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install ruff

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt autoremove \
&& apt clean \
&& rm -rf /var/lib/apt/lists/*
