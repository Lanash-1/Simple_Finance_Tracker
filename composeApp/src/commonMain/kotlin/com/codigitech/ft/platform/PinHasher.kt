package com.codigitech.ft.platform

import kotlin.random.Random

/**
 * PIN storage for a single-user, local-only app. Uses a salted FNV-1a 64 hash iterated
 * many times; not a KDF of cryptographic strength, but enough to keep the PIN out of
 * plain text in the preferences file. The lock is a casual-access deterrent by design.
 */
object PinHasher {
    fun newSalt(): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    fun hash(pin: String, salt: String): String {
        var h = fnv1a("$salt:$pin")
        repeat(20_000) { h = fnv1a("$salt:$h") }
        return h.toULong().toString(16)
    }

    private fun fnv1a(s: String): Long {
        var hash = -0x340d631b7bdddcdbL // FNV offset basis
        for (ch in s) {
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }
}
