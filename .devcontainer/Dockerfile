FROM mcr.microsoft.com/devcontainers/java:1-17

# Install any additional packages
RUN apt-get update && apt-get install -y \
    net-tools \
    iputils-ping \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace