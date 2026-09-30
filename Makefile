# 默认的 Tag 号，如果在执行 make 时没输入，则使用这个
TAG ?= v0.0.1

.PHONY: build clean

# 执行打包的指令
build:
	@echo "正在开始打包，版本号为: $(TAG)"
	flutter build apk --split-per-abi --build-name="$(TAG)"
	mv build/app/outputs/flutter-apk/app-arm64-v8a-release.apk build/app/outputs/flutter-apk/app-arm64-v8a-release-$(TAG).apk

	@echo "打包完成！文件位于 build/app/outputs/flutter-apk/"

# 清理缓存
#clean:
#	flutter clean


## 使用方法：make build TAG=v1.0.0