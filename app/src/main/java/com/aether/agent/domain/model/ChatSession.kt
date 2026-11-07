package com.aether.agent.domain.model

const val DEFAULT_SESSION_TITLE = "Chat baru"

/** Sesi chat milik editor kode. Disembunyikan dari daftar Chat utama. */
const val EDITOR_SESSION_PREFIX = "editor:"

data class ChatSession(val id: String, val title: String, val updatedAt: Long)
