# WristBlocks box: the blocks editor plus everything needed to build and install Wear OS apps.
# Apple Watch apps are built on GitHub's Macs (see .github/workflows/build.yml), never in here.
FROM eclipse-temurin:17-jdk-jammy

ARG CMDLINE_TOOLS=16111833
ENV ANDROID_HOME=/opt/android-sdk \
    GRADLE_USER_HOME=/cache/gradle
ENV PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends python3 unzip curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
 && curl -fsSL -o /tmp/tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip" \
 && unzip -q /tmp/tools.zip -d "$ANDROID_HOME/cmdline-tools" \
 && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
 && rm /tmp/tools.zip \
 && yes | sdkmanager --licenses > /dev/null \
 && sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0" > /dev/null

COPY docker/wristblocks.sh /usr/local/bin/wristblocks
RUN chmod +x /usr/local/bin/wristblocks

WORKDIR /work
EXPOSE 8080
ENTRYPOINT ["wristblocks"]
CMD ["editor"]
