package mypoli.android.pet.usecase

import mypoli.android.TestUtil
import mypoli.android.pet.Pet
import mypoli.android.pet.PetAvatar
import mypoli.android.pet.PetEquipment
import mypoli.android.pet.PetItem
import mypoli.android.player.Inventory
import mypoli.android.player.InventoryPet
import mypoli.android.player.Player
import org.amshove.kluent.`should be null`
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 12/6/17.
 */
class ChangePetUseCaseSpek : Spek({
    describe("ChangePetUseCase") {

        fun executeUseCase(player: Player, pet: PetAvatar) =
            ChangePetUseCase(TestUtil.playerRepoMock(player)).execute(pet)

        it("should require pet in inventory") {
            val player = TestUtil.player().copy(
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet("", PetAvatar.CHICKEN)
                    )
                )
            )
            val exec = { executeUseCase(player, PetAvatar.ELEPHANT) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should change pet") {
            val currentPet = Pet(
                name = "",
                avatar = PetAvatar.ELEPHANT,
                equipment = PetEquipment(PetItem.RED_HAT)
            )
            val newPet = Pet(
                name = "",
                avatar = PetAvatar.CHICKEN
            )
            val player = TestUtil.player().copy(
                pet = currentPet,
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet.createFromPet(currentPet),
                        InventoryPet.createFromPet(newPet)
                    )
                )
            )

            val newPlayer = executeUseCase(player, newPet.avatar)
            newPlayer.pet.avatar.`should be`(newPet.avatar)
            newPlayer.pet.equipment.hat.`should be null`()
        }
    }
})