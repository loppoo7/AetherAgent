package com.aether.agent.domain.model

data class AttachedFileContext(val repository: String, val path: String, val content: String) {
    val id: String get() = "$repository::$path"
}
