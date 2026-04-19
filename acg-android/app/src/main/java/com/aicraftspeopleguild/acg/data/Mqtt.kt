package com.aicraftspeopleguild.acg.data

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kotlin port of guild/Enterprise/L2/mqtt/mqtt.js — topic pub/sub with
 * MQTT-shaped wildcards (`*` = one segment, `#` = rest of path) and
 * retained-last-value replay. In-memory only; the persistence side is
 * [TagDb] and syncs happen through [SyncEngine].
 *
 * Used by the UI to reactively re-render when tags change, and by
 * SyncEngine to fan out every imported tag as an event so screens
 * don't have to poll the DB.
 */
object Mqtt {
    data class Message(val topic: String, val value: Any?, val retained: Boolean, val ts: Long, val from: String?)

    private val subs = CopyOnWriteArrayList<Sub>()
    private val retained = java.util.concurrent.ConcurrentHashMap<String, Message>()
    private val idSeq = AtomicInteger(1)

    private data class Sub(val id: Int, val pattern: String, val regex: Regex,
                           val fn: (Message) -> Unit)

    private fun patternToRegex(pattern: String): Regex {
        val escaped = Regex("[.+?^${'$'}{}()|\\[\\]\\\\]").replace(pattern) { "\\${it.value}" }
        val body = escaped.replace("*", "[^.]+").replace("#", ".*")
        return Regex("^$body$")
    }

    fun subscribe(pattern: String, fn: (Message) -> Unit): () -> Unit {
        val sub = Sub(idSeq.getAndIncrement(), pattern, patternToRegex(pattern), fn)
        subs.add(sub)
        // Replay retained matches to the new subscriber.
        retained.values
            .filter { sub.regex.matches(it.topic) }
            .forEach { runCatching { fn(it) } }
        return { subs.remove(sub) }
    }

    fun publish(topic: String, value: Any?, retained: Boolean = true, from: String? = "android") {
        val m = Message(topic, value, retained, System.currentTimeMillis(), from)
        if (retained) this.retained[topic] = m
        subs.forEach { s ->
            if (s.regex.matches(topic)) runCatching { s.fn(m) }
        }
    }

    fun peek(topic: String): Message? = retained[topic]
    fun clearRetained(topic: String? = null) {
        if (topic == null) retained.clear() else retained.remove(topic)
    }
    fun subCount(): Int = subs.size
    fun retainedCount(): Int = retained.size
    fun matches(pattern: String, topic: String): Boolean = patternToRegex(pattern).matches(topic)
}
