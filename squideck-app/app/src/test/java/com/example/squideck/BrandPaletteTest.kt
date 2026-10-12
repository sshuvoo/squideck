package com.example.squideck

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the brand rules: the six Squid Game palette colours in ui/theme/Color.kt (mirrored in
 * res/values/colors.xml) are the only colours allowed, and all text uses the game font through GameText.
 */
class BrandPaletteTest {
    private val main = File("src/main")
    private val paletteFile = File(main, "java/com/example/squideck/ui/theme/Color.kt")
    private val colorsXml = File(main, "res/values/colors.xml")
    private val brand = setOf("AF0B45", "F44786", "FFFFFF", "249F9C", "037A76", "161313")

    @Test
    fun paletteIsExactlyTheSixBrandColours() {
        val inKotlin = Regex("Color\\(0xFF([0-9A-Fa-f]{6})\\)").findAll(paletteFile.readText()).map { it.groupValues[1].uppercase() }.toSet()
        val inXml = Regex("#FF([0-9A-Fa-f]{6})").findAll(colorsXml.readText()).map { it.groupValues[1].uppercase() }.toSet()
        assertEquals(brand, inKotlin)
        assertEquals(brand, inXml)
    }

    @Test
    fun noHardcodedColoursInKotlin() {
        val forbidden = Regex("Color\\(0x|Color\\.(White|Black|Red|Green|Blue|Yellow|Gray|DarkGray|LightGray|Cyan|Magenta)\\b")
        val offenders = File(main, "java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it != paletteFile }
            .filter { forbidden.containsMatchIn(it.readText()) }
            .map { it.path }
            .toList()
        assertTrue("Use the palette in ui/theme/Color.kt instead of raw colours: $offenders", offenders.isEmpty())
    }

    @Test
    fun allTextUsesTheGameFontInCapitals() {
        val gameText = File(main, "java/com/example/squideck/ui/components/GameText.kt")
        val offenders = File(main, "java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it != gameText }
            .filter { it.readText().contains("import androidx.compose.material3.Text\n") }
            .map { it.path }
            .toList()
        assertTrue("Use GameText (game font, capitals) instead of Material Text: $offenders", offenders.isEmpty())
        assertTrue(File(main, "res/font/game_of_squids.ttf").isFile)
        assertTrue(File(main, "java/com/example/squideck/ui/theme/Type.kt").readText().contains("R.font.game_of_squids"))
    }

    @Test
    fun noHardcodedColoursInResources() {
        val hex = Regex("#[0-9A-Fa-f]{6,8}\\b")
        val offenders = File(main, "res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" && it != colorsXml }
            .filter { hex.containsMatchIn(it.readText()) }
            .map { it.path }
            .toList()
        assertTrue("Reference @color/squid_* instead of raw hex colours: $offenders", offenders.isEmpty())
    }
}
