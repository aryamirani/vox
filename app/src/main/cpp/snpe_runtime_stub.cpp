#include "snpe_runtime_stub.h"

#include <android/log.h>
#include <sstream>

#define VOX_LOG(tag, ...) __android_log_print(ANDROID_LOG_INFO, tag, __VA_ARGS__)

SnpeRuntimeStub& runtimeInstance() {
    static SnpeRuntimeStub instance;
    return instance;
}

void SnpeRuntimeStub::init(const std::string& nativeLibDir) {
    initialized_ = true;
    VOX_LOG("VoxSNPE", "SNPE stub initialized (nativeLibDir=%s)", nativeLibDir.c_str());
    VOX_LOG("VoxSNPE", "Install Qualcomm SNPE SDK into snpe-sdk/ to enable hardware inference.");
}

int64_t SnpeRuntimeStub::loadModel(const std::string& modelPath, const std::string& runtime) {
    int64_t id = nextHandle_++;
    handles_[id] = modelPath;
    VOX_LOG("VoxSNPE", "Loaded model stub handle=%lld path=%s runtime=%s",
            (long long) id, modelPath.c_str(), runtime.c_str());
    return id;
}

void SnpeRuntimeStub::unloadModel(int64_t handleId) {
    handles_.erase(handleId);
}

std::string SnpeRuntimeStub::runWhisper(int64_t handleId, const float* samples, int sampleCount, int sampleRateHz) {
    (void) handleId;
    (void) samples;
    std::ostringstream oss;
    oss << "[stub-transcript samples=" << sampleCount << " rate=" << sampleRateHz << "Hz]";
    return oss.str();
}

std::string SnpeRuntimeStub::runLlm(int64_t handleId, const std::string& prompt, int maxNewTokens) {
    (void) handleId;
    (void) prompt;
    (void) maxNewTokens;
    return R"({"commands":[{"topic":"home/kitchen/light/set","payload":"ON"}]})";
}

void SnpeRuntimeStub::release() {
    handles_.clear();
    initialized_ = false;
}
