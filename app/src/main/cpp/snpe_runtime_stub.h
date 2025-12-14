#pragma once

#include <cstdint>
#include <string>
#include <unordered_map>

/**
 * Stub SNPE runtime used when libSNPE.so is not present in snpe-sdk/.
 * Replace method bodies with SNPE API calls once the SDK is linked.
 */
class SnpeRuntimeStub {
public:
    void init(const std::string& nativeLibDir);
    int64_t loadModel(const std::string& modelPath, const std::string& runtime);
    void unloadModel(int64_t handleId);
    std::string runWhisper(int64_t handleId, const float* samples, int sampleCount, int sampleRateHz);
    std::string runLlm(int64_t handleId, const std::string& prompt, int maxNewTokens);
    void release();

private:
    bool initialized_ = false;
    int64_t nextHandle_ = 1;
    std::unordered_map<int64_t, std::string> handles_;
};

SnpeRuntimeStub& runtimeInstance();
