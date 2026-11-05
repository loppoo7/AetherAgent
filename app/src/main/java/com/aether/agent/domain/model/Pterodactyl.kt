package com.aether.agent.domain.model

data class PteroServer(val id: String, val name: String, val suspended: Boolean)

data class PteroFile(val name: String, val isDir: Boolean, val size: Long)
