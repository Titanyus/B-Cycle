package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class ChromaticColorTest {
    private fun color(value:String)=(0xff000000L or value.removePrefix("#").toLong(16)).toInt()
    @Test fun kotlinMatchesFrozenDesignerResultsAcrossEveryThemeAndExtremes() {
        val rows=javaClass.getResourceAsStream("/chromatic-designer-reference.txt")!!.bufferedReader().use { it.readLines() }
        var checked=0
        for(line in rows.filter { it.isNotBlank() && !it.startsWith("# ") }) {
            val parts=line.split(' ');val (base,light,dark,k,expectedLight)=parts;val expectedDark=parts[5]
            val result=ChromaticColor.palette(color(base),light.toDouble(),dark.toDouble(),k.toDouble())
            assertEquals(color(base),result.base)
            for((actual,expected) in listOf(result.light to color(expectedLight),result.dark to color(expectedDark))) {
                for(shift in listOf(0,8,16)) assertTrue("$line: channel $shift",kotlin.math.abs((actual ushr shift and 255)-(expected ushr shift and 255))<=1)
                assertEquals(255,actual ushr 24)
            }
            checked++
        }
        assertEquals(104,checked)
    }
    @Test fun themeStrengthIsIndependentFromHighlightAndShadowAmounts() {
        val base=color("#bf281b")
        val first=ChromaticColor.palette(base,.4,.5,.6)
        val strong=ChromaticColor.palette(base,.4,.5,2.6)
        assertNotEquals(first.light,strong.light);assertNotEquals(first.dark,strong.dark)
        assertEquals(first.light,ChromaticColor.palette(base,.4,.8,.6).light)
        assertEquals(first.dark,ChromaticColor.palette(base,.8,.5,.6).dark)
        for(k in listOf(0.0,.6,2.6,3.0)) {
            val end=ChromaticColor.palette(base,1.0,1.0,k)
            assertEquals(-1,end.light);assertEquals(color("#000000"),end.dark)
        }
    }
}
