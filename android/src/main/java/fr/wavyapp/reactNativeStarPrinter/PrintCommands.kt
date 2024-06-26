package fr.wavyapp.reactNativeStarPrinter

import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.CutType

sealed interface PrinterCommand

enum class Action: PrinterCommand {
  CUT,
  PARTIAL_CUT,
  FULL_DIRECT,
  PARTIAL_DIRECT,
  PAPER_FEED,
  FEED_LINE,
  PRINT_RULED_LINE;

  companion object {
    @Throws(IllegalArgumentException::class)
    fun fromString(value: String): Action {
      return when (value) {
        "cut" -> CUT
        "partial-cut" -> PARTIAL_CUT
        "full-direct" -> FULL_DIRECT
        "partial-direct" -> PARTIAL_DIRECT
        "paper-feed" -> PAPER_FEED
        "feed-line" -> FEED_LINE
        "print-line-separator" -> PRINT_RULED_LINE
        else -> throw IllegalArgumentException("$value is not a valid printer action")
      }
    }

    @Throws(IllegalArgumentException::class)
    fun toCutType(action: Action): CutType {
      return when (action) {
        CUT -> CutType.Full
        PARTIAL_CUT -> CutType.Partial
        FULL_DIRECT -> CutType.FullDirect
        PARTIAL_DIRECT -> CutType.PartialDirect
        else -> throw IllegalArgumentException("Could not tie $action to a valid CutType")
      }
    }
  }
}

enum class PrintDataType {
  TEXT,
  IMAGE,
  BARCODE;

  companion object {
    @Throws(IllegalArgumentException::class)
    fun fromString(value: String): PrintDataType {
      return when (value) {
        "text" -> TEXT
        "image" -> IMAGE
        "barcode" -> BARCODE
        else -> throw IllegalArgumentException("$value is not a valid print data type")
      }
    }
  }
}

@Throws(IllegalArgumentException::class)
fun alignmentFromString(value: String): Alignment {
  return when (value) {
    "center" -> Alignment.Center
    "left" -> Alignment.Left
    "right" -> Alignment.Right
    else -> throw IllegalArgumentException("Invalid align value $value")
  }
}

data class Style(
  val align: Alignment?,
  val barWidth: Int?,
  val bold: Boolean?,
  val diffusion: Boolean?,
  val threshold: Int?,
  val width: Int?,
  val height: Int?,
  val heightExpansion: Int?,
  val widthExpansion: Int?,
  val underlined: Boolean?
) {
  companion object {
    @Throws(IllegalArgumentException::class)
    fun fromMap(style: Map<*, *>): Style {
      style["align"]?.let {alignValue ->
        if (alignValue !is String) {
          throw IllegalArgumentException("Invalid align value type ${alignValue::class.simpleName}")
        }
      }
      style["barWidth"]?.let {barWidthValue ->
        // As per documentation React Native will convert integers to double https://reactnative.dev/docs/native-modules-android#argument-types
        if (barWidthValue !is Double) {
          throw IllegalArgumentException("Invalid barWidth value type ${barWidthValue::class.simpleName}")
        }
      }
      style["bold"]?.let {boldValue ->
        if (boldValue !is Boolean) {
          throw IllegalArgumentException("Invalid bold value type ${boldValue::class.simpleName}")
        }
      }
      style["diffusion"]?.let {diffusionValue ->
        if (diffusionValue !is Boolean) {
          throw IllegalArgumentException("Invalid diffusion value type ${diffusionValue::class.simpleName}")
        }
      }
      style["threshold"]?.let {thresholdValue ->
        // As per documentation React Native will convert Int to double https://reactnative.dev/docs/native-modules-android#argument-types
        if (thresholdValue !is Double) {
          throw IllegalArgumentException("Invalid threshold value type ${thresholdValue::class.simpleName}")
        }
      }
      style["width"]?.let {widthValue ->
        // As per documentation React Native will convert Int to double https://reactnative.dev/docs/native-modules-android#argument-types
        if (widthValue !is Double) {
          throw IllegalArgumentException("Invalid width value type ${widthValue::class.simpleName}")
        }
      }
      style["height"]?.let {heightValue ->
        // As per documentation React Native will convert Int to double https://reactnative.dev/docs/native-modules-android#argument-types
        if (heightValue !is Double) {
          throw IllegalArgumentException("Invalid height value type ${heightValue::class.simpleName}")
        }
      }
      style["heightExpansion"]?.let {heightExpansionValue ->
        // As per documentation React Native will convert Int to double https://reactnative.dev/docs/native-modules-android#argument-types
        if (heightExpansionValue !is Double) {
          throw IllegalArgumentException("Invalid heightExpansion value type ${heightExpansionValue::class.simpleName}")
        }
      }
      style["widthExpansion"]?.let {widthExpansionValue ->
        // As per documentation React Native will convert Int to Double https://reactnative.dev/docs/native-modules-android#argument-types
        if (widthExpansionValue !is Double) {
          throw IllegalArgumentException("Invalid widthExpansion value type ${widthExpansionValue::class.simpleName}")
        }
      }
      style["underlined"]?.let {underlinedValue ->
        if (underlinedValue !is Boolean) {
          throw IllegalArgumentException("Invalid underlined value type ${underlinedValue::class.simpleName}")
        }
      }

      return Style(
          (style["align"] as? String)?.let{ alignValue -> alignmentFromString(alignValue) },
          (style["barWidth"] as? Double)?.toInt(),
          style["bold"] as Boolean?,
          style["diffusion"] as Boolean?,
          (style["threshold"] as? Double)?.toInt(),
          (style["width"] as? Double)?.toInt(),
          (style["height"] as? Double)?.toInt(),
          (style["heightExpansion"] as? Double)?.toInt(),
          (style["widthExpansion"] as? Double)?.toInt(),
          style["underlined"] as Boolean?,
        )
    }
  }
}

data class Print(val data: String, val type: PrintDataType, val style: Style? = null): PrinterCommand
data class PrinterAction(val action: Action, val args: Map<*, *>?): PrinterCommand

fun printerCommandFactory(command: Map<String, Any?>): PrinterCommand {
  (command["data"] as? String)?.let { data ->
    return Print(
      data,
      (command["type"] as? String)?.let { type ->  PrintDataType.fromString(type) } ?: PrintDataType.TEXT,
      (command["style"] as? Map<*, *>)?.let { style -> Style.fromMap(style) }
    )
  }
  (command["action"] as? String)?.let { action ->
    return PrinterAction(Action.fromString(action), command["actionArguments"] as? Map<*,*>)
  }
  throw IllegalArgumentException("The command $command has no data nor action")
}