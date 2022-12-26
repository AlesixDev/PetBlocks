package com.github.shynixn.petblocks.bukkit.contract

import com.github.shynixn.mcutils.common.Item
import com.github.shynixn.petblocks.bukkit.entity.PetVisibility
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface Pet {
    /**
     * Identifier. Each pet of a player needs to have a unique identifier how to interact with it.
     */
    val name: String

    /**
     * Gets or sets the displayName of the pet. ChatColors are automatically translated to be visible in game.
     */
    var displayName: String

    /**
     * Gets if the pet is currently atleast visible to the owner.
     * Check [visibility] to see if other players can see the pet as well.
     */
    val isSpawned: Boolean

    /**
     * Gets the owner of the pet.
     */
    val player: Player

    /**
     * Gets or sets the current location of the pet.
     * Can also be used while the pet is not spawned.
     */
    var location: Location

    /**
     * Gets or sets the visibility of the pet.
     * Should be used together with [isSpawned] to check if a pet can really be seen by a player at the moment.
     */
    var visibility: PetVisibility

    /**
     * Gets or sets the itemStack being rendered.
     */
    var headItemStack : ItemStack

    /**
     * Gets or sets the itemStack in ItemFormat.
     */
    var headItem : Item

    /**
     * Gets if the pet has been disposed. The pet can no longer be used then.
     */
    val isDisposed: Boolean

    /**
     * Calls the pet to the player. Spawns the pet if it is not spawned, and places the pet
     * right in front of the player.
     */
    fun call()

    /**
     * DeSpawns the pet for the owner and other players.
     * The current location of the pet is stored.
     */
    fun remove()

    /**
     * Shows the pet for the owner (and other players depending on the visibility) at the location
     * the pet remembers.
     */
    fun spawn()

    /**
     * Starts riding the pet on the ground.
     * Spawns the pet if it is not spawned.
     */
    fun ride()

    /**
     * Starts flying the pet around.
     * Spawns the pet if it is not spawned.
     */
    fun fly()

    /**
     * Starts wearing the pet as a hat.
     * Spawns the pet if it is not spawned.
     */
    fun hat()

    /**
     * Stops riding, flying or hat if the pet currently performs it.
     */
    fun umount()

    /**
     * Is the owner riding on the pet.
     */
    fun isRiding(): Boolean

    /**
     * Is the owner flying on the pet.
     */
    fun isFlying(): Boolean

    /**
     * Is owner wearing the pet on its head?
     */
    fun isHat(): Boolean

    /**
     * Is the pet mounted as a hat or is someone riding or flying it?
     */
    fun isMounted(): Boolean

    /**
     * Permanently disposes this pet. Once disposed, this instance can no longer be used.
     * The pet is not deleted however and can be retrieved again from the [PetService].
     */
    fun dispose()
}
