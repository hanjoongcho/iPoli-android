package mypoli.android.player.usecase

import mypoli.android.TestUtil
import mypoli.android.player.Inventory
import mypoli.android.player.Player
import mypoli.android.player.Theme
import mypoli.android.store.theme.usecase.BuyThemeUseCase
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 12/12/17.
 */
class BuyThemeUseCaseSpek : Spek({

    describe("BuyThemeUseCaseSpek") {

        fun executeUseCase(player: Player, theme: Theme) =
            BuyThemeUseCase(TestUtil.playerRepoMock(player)).execute(theme)


        it("should require not bought theme") {
            val player = TestUtil.player().copy(
                inventory = Inventory(themes = setOf(Theme.BLUE))
            )
            val exec = { executeUseCase(player, Theme.BLUE) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should not buy when not enough gems") {
            val player = TestUtil.player().copy(
                gems = Theme.BLUE.gemPrice - 1,
                inventory = Inventory()
            )
            val result = executeUseCase(player, Theme.BLUE)
            result.`should be`(BuyThemeUseCase.Result.TooExpensive)
        }

        it("should buy theme") {
            val player = TestUtil.player().copy(
                gems = Theme.BLUE.gemPrice,
                inventory = Inventory()
            )
            val result = executeUseCase(player, Theme.BLUE)
            result.`should be instance of`(BuyThemeUseCase.Result.ThemeBought::class)
            val newPlayer = (result as BuyThemeUseCase.Result.ThemeBought).player
            newPlayer.gems.`should be equal to`(player.gems - Theme.BLUE.gemPrice)
            newPlayer.hasTheme(Theme.BLUE).`should be true`()
        }
    }
})