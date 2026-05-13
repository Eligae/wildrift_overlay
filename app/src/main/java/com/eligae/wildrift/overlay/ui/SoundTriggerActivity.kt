package com.eligae.wildrift.overlay.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.audio.AudioCaptureSession
import com.eligae.wildrift.overlay.audio.AudioSessionHolder
import com.eligae.wildrift.overlay.audio.MelFingerprint
import com.eligae.wildrift.overlay.audio.SoundTemplate
import com.eligae.wildrift.overlay.audio.SoundTemplateRepo
import com.eligae.wildrift.overlay.audio.TriggerAction

/**
 * 사운드 트리거 학습 화면.
 *  - 캡처 서비스가 가동 중이어야 ring buffer 사용 가능.
 *  - "방금 들렸어요" → 직전 4초 PCM snapshot → mel fingerprint 계산 → 라벨/액션 입력 → 저장.
 *  - 등록된 템플릿 (builtin + user) 리스트 표시, user 항목은 길게 눌러 삭제.
 */
class SoundTriggerActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var btnCapture: Button
    private lateinit var btnSave: Button
    private lateinit var inputLabel: EditText
    private lateinit var spinAction: Spinner
    private lateinit var list: RecyclerView
    private val adapter = TriggerAdapter(::onItemLongClick)

    private var pendingFeatures: Array<FloatArray>? = null
    private val actionLabels = listOf(
        "매치 시작 (MATCH_START)",
        "매치 승리 (MATCH_WIN)",
        "매치 패배 (MATCH_LOSE)",
        "스킬 ready (ULT_READY)",
        "채팅 알림 — 즉시 OCR (CHAT_PING)",
        "커스텀 (CUSTOM)",
    )
    private val actionValues = listOf(
        TriggerAction.MATCH_START, TriggerAction.MATCH_WIN, TriggerAction.MATCH_LOSE,
        TriggerAction.ULT_READY, TriggerAction.CHAT_PING, TriggerAction.CUSTOM,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_trigger)
        status = findViewById(R.id.sound_status)
        btnCapture = findViewById(R.id.btn_capture_now)
        btnSave = findViewById(R.id.btn_save)
        inputLabel = findViewById(R.id.input_label)
        spinAction = findViewById(R.id.spin_action)
        list = findViewById(R.id.trigger_list)

        spinAction.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actionLabels)

        btnCapture.setOnClickListener { captureNow() }
        btnSave.setOnClickListener { saveCurrent() }

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        refresh()

        val active = AudioSessionHolder.session
        status.text = if (active?.isRunning() == true) {
            "캡처 서비스 가동 중 — 게임 사운드 들려오면 버튼 누르세요."
        } else {
            "캡처 서비스가 꺼져 있습니다. 메인 화면에서 '캡처 시작' 먼저."
        }
    }

    private fun refresh() {
        adapter.submit(SoundTemplateRepo.loadAll(this))
    }

    private fun captureNow() {
        val session: AudioCaptureSession? = AudioSessionHolder.session
        if (session == null || !session.isRunning()) {
            Toast.makeText(this, "캡처 서비스가 꺼져 있어요 — 메인 화면에서 시작하세요.", Toast.LENGTH_LONG).show()
            return
        }
        val pcm = session.snapshot(seconds = 4)
        if (pcm.size < MelFingerprint.FRAME_SIZE) {
            Toast.makeText(this, "오디오가 아직 충분히 쌓이지 않음 — 캡처 시작 후 5초 대기", Toast.LENGTH_SHORT).show()
            return
        }
        val features = MelFingerprint.compute(pcm)
        if (features.isEmpty()) {
            Toast.makeText(this, "fingerprint 계산 실패", Toast.LENGTH_SHORT).show()
            return
        }
        pendingFeatures = features
        btnSave.isEnabled = true
        status.text = "캡처됨: ${pcm.size / MelFingerprint.SAMPLE_RATE.toFloat()}s, frames=${features.size}. 라벨/액션 입력 후 저장."
    }

    private fun saveCurrent() {
        val feats = pendingFeatures ?: return
        val label = inputLabel.text.toString().trim()
        if (label.isEmpty()) {
            Toast.makeText(this, "라벨을 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }
        val action = actionValues[spinAction.selectedItemPosition]
        val id = "user_${System.currentTimeMillis()}"
        val template = SoundTemplate(
            id = id, label = label, action = action, threshold = 0.85f,
            features = feats, builtin = false,
        )
        SoundTemplateRepo.saveUserTemplate(this, template)
        AudioSessionHolder.detector?.reload()
        Toast.makeText(this, "저장됨: $label", Toast.LENGTH_SHORT).show()
        pendingFeatures = null
        btnSave.isEnabled = false
        inputLabel.text.clear()
        refresh()
    }

    private fun onItemLongClick(t: SoundTemplate) {
        if (t.builtin) {
            Toast.makeText(this, "builtin 항목은 삭제 불가", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("삭제")
            .setMessage("'${t.label}' 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                SoundTemplateRepo.deleteUserTemplate(this, t.id)
                AudioSessionHolder.detector?.reload()
                refresh()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private class TriggerAdapter(
        private val onLongClick: (SoundTemplate) -> Unit,
    ) : RecyclerView.Adapter<TriggerAdapter.VH>() {
        private var items: List<SoundTemplate> = emptyList()
        fun submit(list: List<SoundTemplate>) {
            items = list; notifyDataSetChanged()
        }
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val sub: TextView = view.findViewById(android.R.id.text2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(
                android.R.layout.simple_list_item_2, parent, false,
            )
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = items[position]
            val tag = if (t.builtin) "🔒" else "⚙"
            holder.title.text = "$tag  ${t.label}"
            holder.sub.text = "${t.action.name} · ${"%.1f".format(t.durationSec())}s · ${t.frames} frames"
            holder.itemView.setOnLongClickListener { onLongClick(t); true }
        }
        override fun getItemCount(): Int = items.size
    }
}
