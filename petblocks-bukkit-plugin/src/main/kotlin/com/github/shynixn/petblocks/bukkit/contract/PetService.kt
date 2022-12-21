package com.github.shynixn.petblocks.bukkit.contract

import com.github.shynixn.petblocks.bukkit.entity.PetSpawnResult
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletionStage

interface PetService {
    /**
     * Gets all the pets a player owns.
     * The pets may or be not be spawned at the moment.
     */
    suspend fun getPetsFromPlayer(player: Player): List<Pet>

    /**
     * Creates a pet for the given player, at the given location, with the given template.
     * Throws an exception if template id does not exist.
     */
    suspend fun createPet(player: Player, location: Location, templateId: String): PetSpawnResult

    /**
     * Adds a pet for the given player, at the given location, with the given template.
     * Throws an exception if template id does not exist.
     */
    fun createPetAsync(player: Player, location: Location, templateId: String): CompletionStage<PetSpawnResult>

    /**
     * Gets all the pets a player owns.
     * The pets may or be not be spawned at the moment.
     */
    fun getPetsFromPlayerAsync(player: Player): CompletionStage<List<Pet>>

    /**
     *  Deletes the given pet.
     */
    suspend fun deletePet(pet: Pet): CompletionStage<Void>
}
