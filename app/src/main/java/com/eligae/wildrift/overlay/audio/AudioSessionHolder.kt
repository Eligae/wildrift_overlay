package com.eligae.wildrift.overlay.audio

/**
 * Service의 AudioCaptureSession 인스턴스를 다른 Activity(예: SoundTriggerActivity)에서 접근.
 * Singleton, ScreenCaptureService start/stop에서 set.
 */
object AudioSessionHolder {
    @Volatile
    var session: AudioCaptureSession? = null

    /** SoundDetector — Activity가 새 템플릿 저장 후 reload 호출용. */
    @Volatile
    var detector: SoundDetector? = null
}
