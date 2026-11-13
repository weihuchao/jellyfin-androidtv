# Jellyfin Android TV 定制版：构建、导出与安装
#
# 目标：
#   make build                仅构建 release APK
#   make export               构建并把 APK 导出到 release/
#   make export DEST=dist/    构建并把 APK 导出到指定目录
#   make install              导出后通过 adb 覆盖安装到投影仪
#   make version              打印当前使用的版本号
#
# 可覆盖变量：
#   VERSION   版本号，默认取环境变量 JELLYFIN_VERSION，否则 0.20.0-seamless-v0.7
#   DEST      APK 导出目录，默认 release
#   DEVICE    投影仪的 adb 地址，默认 192.168.55.80:5555

SHELL := /bin/bash
.DEFAULT_GOAL := help

DEVICE  ?= 192.168.55.80:5555
DEST    ?= release
VERSION ?= $(if $(JELLYFIN_VERSION),$(JELLYFIN_VERSION),0.20.0-seamless-v0.7)

GRADLE       := ./gradlew
APK_DIR      := app/build/outputs/apk/release
APK_NAME     := jellyfin-androidtv-v$(VERSION)-release.apk
EXPORTED_APK := $(patsubst %/,%,$(DEST))/$(APK_NAME)

# 未显式配置 Android SDK 时，回退到 macOS 的默认安装位置
ifeq ($(strip $(ANDROID_HOME)),)
ifneq ($(wildcard $(HOME)/Library/Android/sdk),)
export ANDROID_HOME := $(HOME)/Library/Android/sdk
endif
endif

# Gradle 的 toolchain 要求 JDK 21（见 .tools-versions），把候选的 JDK 都交给 Gradle 自行挑选
comma := ,
space := $(empty) $(empty)
JDK_PATHS := $(strip $(wildcard $(JAVA_HOME)) $(wildcard /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home))
JAVA_INSTALLATIONS := $(subst $(space),$(comma),$(JDK_PATHS))

.PHONY: help build export install version clean

help:
	@echo 'Jellyfin Android TV 定制版构建工具'
	@echo
	@echo '  make build              构建 release APK'
	@echo '  make export             构建并导出 APK 到 $(DEST)/'
	@echo '  make export DEST=dist/  构建并导出 APK 到指定目录'
	@echo '  make install            导出后覆盖安装到投影仪 $(DEVICE)'
	@echo '  make version            打印当前使用的版本号'
	@echo
	@echo '当前变量：VERSION=$(VERSION) DEST=$(DEST) DEVICE=$(DEVICE)'

version:
	@echo $(VERSION)

build:
	$(GRADLE) :app:assembleRelease "-Pjellyfin.version=$(VERSION)" "-Porg.gradle.java.installations.paths=$(JAVA_INSTALLATIONS)"

export: build
	@mkdir -p "$(DEST)"
	@if [ ! -f "$(APK_DIR)/$(APK_NAME)" ]; then \
		echo '错误：没有找到构建产物 $(APK_DIR)/$(APK_NAME)'; \
		echo '实际产物：'; \
		ls -1 "$(APK_DIR)" 2>/dev/null || true; \
		exit 1; \
	fi
	@cp -f "$(APK_DIR)/$(APK_NAME)" "$(EXPORTED_APK)"
	@echo '已导出 $(EXPORTED_APK)'

install: export
	@adb connect "$(DEVICE)" >/dev/null 2>&1 || true
	@adb -s "$(DEVICE)" get-state >/dev/null 2>&1 || { echo '错误：设备 $(DEVICE) 未连接'; exit 1; }
	@if ! adb -s "$(DEVICE)" install -r "$(EXPORTED_APK)"; then \
		echo '提示：若报 INSTALL_FAILED_UPDATE_INCOMPATIBLE，说明签名与已安装版本不同，需先卸载：'; \
		echo '  adb -s $(DEVICE) uninstall org.jellyfin.androidtv'; \
		exit 1; \
	fi

clean:
	$(GRADLE) clean
