package com.aether.agent.domain.model

/** Seberapa jauh Agen AI boleh menyentuh file dan power server Pterodactyl. */
enum class AgentMode(val label: String, val hint: String) {
    OFF("Mati", "AI tidak bisa menyentuh file server."),
    ASSIST("Bantu", "AI membaca dan mengusulkan. Ubah tetap lewat persetujuan."),
    ACT("Lakukan", "AI boleh menulis dan merestart. Setiap tindakan tetap menunggu persetujuanmu."),
}
