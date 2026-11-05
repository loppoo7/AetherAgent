package com.aether.agent.domain.model

data class GithubRepo(
    val owner: String,
    val name: String,
    val isPrivate: Boolean,
    val description: String?,
    val defaultBranch: String,
    /** false jika akun hanya punya akses baca. */
    val canPush: Boolean,
)

fun GithubRepo.toTargetKey(): String = "$owner/$name|$defaultBranch"
