RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  autoconf \
  devscripts \
  dia \
  graphviz \
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

RUN export DEBIAN_FRONTEND=noninteractive \
&& wget -qO- https://astral.sh/uv/install.sh | sh
ENV PATH="/root/.local/bin:$PATH"

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  npm

RUN export DEBIAN_FRONTEND=noninteractive \
&& npm install markdownlint-cli2 --global
