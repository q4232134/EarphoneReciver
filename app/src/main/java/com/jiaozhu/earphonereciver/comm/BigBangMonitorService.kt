package com.jiaozhu.earphonereciver.comm

import android.accessibilityservice.AccessibilityService
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


class BigBangMonitorService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val type = event.eventType
        val className = event.className
        println(type)

        val info = event.source ?: return
        rootInActiveWindow.findAccessibilityNodeInfosByViewId("root").forEach { println(it) }
        var txt = info.text
        println(txt)
        println(event.text)
        println(info.findAccessibilityNodeInfosByViewId("root"))
        when (type) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_CLICKED -> {
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {

            }
        }
    }


    fun findViewByText(text: String?): AccessibilityNodeInfo? {
        val accessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text)
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (nodeInfo in nodeInfoList) {
                println(nodeInfo?.text)
            }
        }
        return null
    }

    override fun onInterrupt() {}

}