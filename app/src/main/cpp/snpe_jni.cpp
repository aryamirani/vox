#include "snpe_runtime_stub.h"

#include <jni.h>
#include <string>
#include <vector>

extern "C" {

JNIEXPORT void JNICALL
Java_com_edgeai_vox_snpe_SnpeNative_init(JNIEnv* env, jobject /* thiz */, jstring nativeLibDir) {
    const char* dir = env->GetStringUTFChars(nativeLibDir, nullptr);
    runtimeInstance().init(std::string(dir));
    env->ReleaseStringUTFChars(nativeLibDir, dir);
}

JNIEXPORT jlong JNICALL
Java_com_edgeai_vox_snpe_SnpeNative_loadModel(JNIEnv* env, jobject /* thiz */, jstring modelPath, jstring runtime) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    const char* rt = env->GetStringUTFChars(runtime, nullptr);
    int64_t handle = runtimeInstance().loadModel(std::string(path), std::string(rt));
    env->ReleaseStringUTFChars(modelPath, path);
    env->ReleaseStringUTFChars(runtime, rt);
    return static_cast<jlong>(handle);
}

JNIEXPORT void JNICALL
Java_com_edgeai_vox_snpe_SnpeNative_unloadModel(JNIEnv* /* env */, jobject /* thiz */, jlong handleId) {
    runtimeInstance().unloadModel(static_cast<int64_t>(handleId));
}

JNIEXPORT jstring JNICALL
Java_com_edgeai_vox_snpe_SnpeNative_runWhisper(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handleId,
    jfloatArray audioSamples,
    jint sampleRateHz
) {
    jsize length = env->GetArrayLength(audioSamples);
    std::vector<float> buffer(static_cast<size_t>(length));
    env->GetFloatArrayRegion(audioSamples, 0, length, buffer.data());

    std::string transcript = runtimeInstance().runWhisper(
        static_cast<int64_t>(handleId),
        buffer.data(),
        length,
        sampleRateHz
    );
    return env->NewStringUTF(transcript.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_edgeai_vox_snpe_SnpeNative_runLlm(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handleId,
    jstring prompt,
    jint maxNewTokens
) {
    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    std::string output = runtimeInstance().runLlm(
        static_cast<int64_t>(handleId),
        std::string(promptChars),
        maxNewTokens
    );
    env->ReleaseStringUTFChars(prompt, promptChars);
    return env->NewStringUTF(output.c_str());
}

JNIEXPORT void JNICALL
Java_com_edgeai_vox_snpe_SnpeNative_release(JNIEnv* /* env */, jobject /* thiz */) {
    runtimeInstance().release();
}

}
