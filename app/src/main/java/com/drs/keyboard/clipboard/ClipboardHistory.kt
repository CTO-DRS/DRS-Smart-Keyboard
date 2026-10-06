package com.drs.keyboard.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Clipboard hub: remembers recent copies (sensitive clips are skipped),
 * supports pinning, and serves the paste panel inside the keyboard.
 */
class ClipboardHistory(context: Context) : ClipboardManager.OnPrimaryClipChangedListener {

    data class Item(val text: String, val pinned: Boolean, val time: Long)

    interface Listener {
        fun onChanged()
    }

    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val items = ArrayList<Item>(32)
    private val listeners = ArrayList<Listener>(1)
    var enabled = true

    init {
        load()
        cm.addPrimaryClipChangedListener(this)
    }

    fun addListener(l: Listener) {
        if (!listeners.contains(l)) listeners.add(l)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    override fun onPrimaryClipChanged() {
        if (!enabled) return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val desc = clip.description
        val extras = desc.extras
        if (extras != null && extras.getBoolean("android.content.extra.IS_SENSITIVE", false)) return
        val text = clip.getItemAt(0).coerceToText(appContext)?.toString() ?: return
        if (text.isEmpty() || text.length > 2000) return
        addInternal(text)
    }

    private fun addInternal(text: String) {
        val wasPinned = items.firstOrNull { it.text == text }?.pinned ?: false
        items.removeAll { it.text == text }
        items.add(0, Item(text, wasPinned, System.currentTimeMillis()))
        while (items.size > 30) {
            // drop oldest unpinned first
            val idx = items.indexOfLast { !it.pinned }
            if (idx >= 0) items.removeAt(idx) else items.removeAt(items.size - 1)
        }
        save()
        listeners.forEach { it.onChanged() }
    }

    fun all(): List<Item> = items.sortedWith(compareByDescending { it.pinned })

    fun setPinned(text: String, pinned: Boolean) {
        val idx = items.indexOfFirst { it.text == text }
        if (idx >= 0) {
            items[idx] = items[idx].copy(pinned = pinned)
            save()
            listeners.forEach { it.onChanged() }
        }
    }

    fun remove(text: String) {
        items.removeAll { it.text == text }
        save()
        listeners.forEach { it.onChanged() }
    }

    fun clearAll() {
        items.clear()
        save()
        listeners.forEach { it.onChanged() }
    }

    /** Privacy auto-clear: drop unpinned history, keep pinned items. */
    fun clearUnpinned() {
        items.removeAll { !it.pinned }
        save()
        listeners.forEach { it.onChanged() }
    }

    /** Wipes the system clipboard (API 26+; minSdk is 26). */
    fun clearSystem() {
        runCatching { cm.clearPrimaryClip() }
    }

    fun copyToSystem(text: String) {
        cm.setPrimaryClip(ClipData.newPlainText("drs", text))
    }

    private fun save() {
        val prefs = appContext.getSharedPreferences("drs_clip", Context.MODE_PRIVATE)
        val pins = items.filter { it.pinned }.map { it.text }
        prefs.edit().putString("pins", pins.joinToString("\u0001"))
            .putString("last", items.firstOrNull()?.text ?: "")
            .apply()
    }

    private fun load() {
        val prefs = appContext.getSharedPreferences("drs_clip", Context.MODE_PRIVATE)
        val last = prefs.getString("last", null)
        if (!last.isNullOrEmpty()) items.add(Item(last, false, 0))
        prefs.getString("pins", null)?.split('\u0001')?.forEach { pin ->
            if (pin.isNotEmpty() && items.none { it.text == pin }) {
                items.add(0, Item(pin, true, 0))
            }
        }
        // mark pinned ones
        val pins = prefs.getString("pins", null)?.split('\u0001')?.toSet() ?: emptySet()
        for (i in items.indices) {
            if (items[i].text in pins) items[i] = items[i].copy(pinned = true)
        }
    }
}
