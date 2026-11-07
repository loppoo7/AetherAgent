package com.aether.agent.domain.agent

/** Membatasi tool yang boleh dipakai dalam satu run agen. */
enum class ToolScope {
    /** Semua tool: GitHub, server, dan lainnya. Dipakai di Chat utama. */
    ALL,
    /** Hanya tool server. Dipakai di chat editor file server. */
    SERVER_ONLY,
}
