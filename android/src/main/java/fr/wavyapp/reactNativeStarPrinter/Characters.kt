package fr.wavyapp.reactNativeStarPrinter

import com.starmicronics.stario10.starxpandcommand.display.InternationalCharacterType as DisplayInternationalCharacterType
import com.starmicronics.stario10.starxpandcommand.printer.InternationalCharacterType as PrinterInternationalCharacterType

fun printerInternationalCharacterTypeFromString(value: String? = "usa"): PrinterInternationalCharacterType {
  return when (value) {
    "usa" -> PrinterInternationalCharacterType.Usa
    "france" -> PrinterInternationalCharacterType.France
    "germany" -> PrinterInternationalCharacterType.Germany
    "uk" -> PrinterInternationalCharacterType.UK
    "denmark" -> PrinterInternationalCharacterType.Denmark
    "sweden" -> PrinterInternationalCharacterType.Sweden
    "italy" -> PrinterInternationalCharacterType.Italy
    "spain" -> PrinterInternationalCharacterType.Spain
    "japan" -> PrinterInternationalCharacterType.Japan
    "norway" -> PrinterInternationalCharacterType.Norway
    "denmark2" -> PrinterInternationalCharacterType.Denmark2
    "spain2" -> PrinterInternationalCharacterType.Spain2
    "latinAmerica" -> PrinterInternationalCharacterType.LatinAmerica
    "korea" -> PrinterInternationalCharacterType.Korea
    "ireland" -> PrinterInternationalCharacterType.Ireland
    "slovenia" -> PrinterInternationalCharacterType.Slovenia
    "croatia" -> PrinterInternationalCharacterType.Croatia
    "china" -> PrinterInternationalCharacterType.China
    "vietnam" -> PrinterInternationalCharacterType.Vietnam
    "arabic" -> PrinterInternationalCharacterType.Arabic
    "legal" -> PrinterInternationalCharacterType.Legal
    else -> PrinterInternationalCharacterType.Usa
  }
}

fun displayInternationalCharacterTypeFromString(value: String? = "usa"): DisplayInternationalCharacterType {
  return when (value) {
    "usa" -> DisplayInternationalCharacterType.Usa
    "france" -> DisplayInternationalCharacterType.France
    "germany" -> DisplayInternationalCharacterType.Germany
    "uk" -> DisplayInternationalCharacterType.UK
    "denmark" -> DisplayInternationalCharacterType.Denmark
    "sweden" -> DisplayInternationalCharacterType.Sweden
    "italy" -> DisplayInternationalCharacterType.Italy
    "spain" -> DisplayInternationalCharacterType.Spain
    "japan" -> DisplayInternationalCharacterType.Japan
    "norway" -> DisplayInternationalCharacterType.Norway
    "denmark2" -> DisplayInternationalCharacterType.Denmark2
    "spain2" -> DisplayInternationalCharacterType.Spain2
    "latinAmerica" -> DisplayInternationalCharacterType.LatinAmerica
    "korea" -> DisplayInternationalCharacterType.Korea
    else -> DisplayInternationalCharacterType.Usa
  }
}