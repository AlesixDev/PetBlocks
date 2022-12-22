package com.github.shynixn.petblocks.bukkit.contract

import com.github.shynixn.petblocks.bukkit.entity.PetTemplate

interface PetTemplateRepository {
    /**
     * Creates all templates if they do not exist yet.
     */
    suspend fun copyTemplatesIfNotExist()

    /**
     * Gets all templates from the repository.
     */
    suspend fun getAll(): List<PetTemplate>
}
