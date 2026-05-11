package com.eligae.wrspellcheck

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityChatService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val texts = mutableListOf<String>()
        val source = event.source
        if (source != null) {
            collectText(source, texts)
        } else {
            rootInActiveWindow?.let { collectText(it, texts) }
        }
        val typeName = eventTypeName(event.eventType)
        if (texts.isNotEmpty()) {
            Log.d(TAG, "[$typeName pkg=${event.packageName}] ${texts.joinToString(" | ")}")
        } else {
            Log.d(TAG, "[$typeName pkg=${event.packageName}] <no text>")
        }
    }

    override fun onInterrupt() {}

    private fun collectText(node: AccessibilityNodeInfo, acc: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { acc.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { acc.add("(desc) $it") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, acc) }
        }
    }

    private fun eventTypeName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WIN_CONTENT_CHG"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT_CHG"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLLED"
        AccessibilityEvent.TYPE_ANNOUNCEMENT -> "ANNOUNCEMENT"
        else -> "0x${type.toString(16)}"
    }

    companion object {
        private const val TAG = "WRChatAcc"
    }
}
