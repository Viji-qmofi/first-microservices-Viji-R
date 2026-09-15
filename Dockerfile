FROM maven:3.9-eclipse-temurin-21

WORKDIR /workspace

RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get update && apt-get install -y \
    curl \
    git \
    bash \
    ca-certificates \
    nano \
    procps \
    nodejs \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Python deps for the course MCP server (mcp/coursetools_server.py) -- only what
# that script actually imports, not the full Module 3.2 requirements.txt list.
RUN pip3 install --no-cache-dir --break-system-packages fastmcp "mcp<2" starlette uvicorn sqlite-vec sentence-transformers

# Install Claude Code
RUN npm install -g @anthropic-ai/claude-code

# Install OpenCode
RUN npm install -g opencode-ai

# Git identity for commits made inside the container
RUN git config --global user.name "viji-qmofi" && \
    git config --global user.email "vijiramu@gmail.com"

# Claude Code configuration: default settings + status line
RUN mkdir -p /root/.claude
COPY settings.json /root/.claude/settings.json
COPY statusline.sh /root/.claude/statusline.sh
RUN chmod +x /root/.claude/statusline.sh

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Student shell quality-of-life improvements
RUN echo 'export PS1="ai-course:\\w# "' >> /root/.bashrc && \
    echo 'alias ll="ls -alF"' >> /root/.bashrc && \
    echo 'alias la="ls -A"' >> /root/.bashrc && \
    echo 'alias l="ls -CF"' >> /root/.bashrc && \
    echo 'alias mci="mvn clean install"' >> /root/.bashrc

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["/bin/bash"]
