# Dependencies for developing the Checker Framework.
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

# Install uv (manages Python dependencies).
RUN curl -LsSf https://astral.sh/uv/install.sh | sh
ENV PATH=/root/.local/bin:$PATH

## TODO: Remove this block (after clients have had time to pull), since we use `uv` instead.
# Old Python dependencies (to remove).
# `pipx ensurepath` only adds to the path in newly-started shells.
# BUT, setting the path for the current user is not enough.
# Azure creates a new user and runs jobs as it.
# So, install into /usr/local/bin which is already on every user's path.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  pipx \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install black \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install flake8 \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install html5validator \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install mypy \
&& PIPX_HOME=/opt/pipx PIPX_BIN_DIR=/usr/local/bin pipx install ruff

# Install markdownlint.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  npm
RUN export DEBIAN_FRONTEND=noninteractive \
&& npm install markdownlint-cli2 --global
