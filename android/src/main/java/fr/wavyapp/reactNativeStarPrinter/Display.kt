package fr.wavyapp.reactNativeStarPrinter

import com.starmicronics.stario10.starxpandcommand.display.Contrast
import com.starmicronics.stario10.starxpandcommand.display.CursorState

fun getDisplayContrast(value: Double): Contrast {
  return when (value) {
    -3.0 -> Contrast.Minus3
    -2.0 -> Contrast.Minus2
    -1.0 -> Contrast.Minus1
    1.0 -> Contrast.Plus1
    2.0 -> Contrast.Plus2
    3.0 -> Contrast.Plus3
    else -> Contrast.Default
  }
}

fun cursorStateFromString(value: String): CursorState {
  return when (value) {
    "on" -> CursorState.On
    "blink" -> CursorState.Blink
    else -> CursorState.Off
  }
}