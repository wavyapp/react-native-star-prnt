package fr.wavyapp.reactNativeStarPrinter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.starmicronics.stario10.DisplayDelegate
import com.starmicronics.stario10.DrawerDelegate
import com.starmicronics.stario10.InputDeviceDelegate
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.PrinterDelegate
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarDeviceDiscoveryManagerFactory
import com.starmicronics.stario10.StarIO10ArgumentException
import com.starmicronics.stario10.StarIO10BadResponseException
import com.starmicronics.stario10.StarIO10CommunicationException
import com.starmicronics.stario10.StarIO10ErrorCode
import com.starmicronics.stario10.StarIO10Exception
import com.starmicronics.stario10.StarIO10IllegalHostDeviceStateException
import com.starmicronics.stario10.StarIO10InUseException
import com.starmicronics.stario10.StarIO10InvalidOperationException
import com.starmicronics.stario10.StarIO10NotFoundException
import com.starmicronics.stario10.StarIO10UnknownException
import com.starmicronics.stario10.StarIO10UnprintableException
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DisplayBuilder
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.DrawerBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.drawer.Channel
import com.starmicronics.stario10.starxpandcommand.drawer.OpenParameter
import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.BarcodeParameter
import com.starmicronics.stario10.starxpandcommand.printer.BarcodeSymbology
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter
import com.starmicronics.stario10.starxpandcommand.printer.LineStyle
import com.starmicronics.stario10.starxpandcommand.printer.RuledLineParameter
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class Events {
  DisplayConnected,
  DisplayCommunicationError,
  DisplayDisconnected,
  InputDeviceCommunicationError,
  InputDeviceConnected,
  InputDeviceDisconnected,
  InputDeviceReadData,
  PrinterCommunicationError,
  PrinterDrawerCommunicationError,
  PrinterDrawerClosed,
  PrinterDrawerOpened,
  PrinterIsReady,
  PrinterHasError,
  PrinterPaperIsReady,
  PrinterPaperIsNearEmpty,
  PrinterPaperIsEmpty,
  PrinterCoverOpened,
  PrinterCoverClosed
}

class ReactStarPrinterModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(
    reactContext
  ) {
  private var discoveryManager: StarDeviceDiscoveryManager? = null
  private var listeners = 0
  private var starPrinter: StarPrinter? = null
  private val starPrinterCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val printerDelegate = object : PrinterDelegate() {
    override fun onCommunicationError(e: StarIO10Exception) {
      super.onCommunicationError(e)
      sendEvent(Events.PrinterCommunicationError.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onReady() {
      super.onReady()
      sendEvent(Events.PrinterIsReady.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onError() {
      super.onError()
      sendEvent(Events.PrinterHasError.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onPaperReady() {
      super.onPaperReady()
      sendEvent(Events.PrinterPaperIsReady.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onPaperNearEmpty() {
      super.onPaperNearEmpty()
      sendEvent(Events.PrinterPaperIsNearEmpty.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onPaperEmpty() {
      super.onPaperEmpty()
      sendEvent(Events.PrinterPaperIsEmpty.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onCoverOpened() {
      super.onCoverOpened()
      sendEvent(Events.PrinterCoverOpened.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onCoverClosed() {
      super.onCoverClosed()
      sendEvent(Events.PrinterCoverClosed.toString().replaceFirstChar { fc -> fc.lowercase() })
    }
  }
  private val drawerDelegate = object : DrawerDelegate() {
    override fun onOpenCloseSignalSwitched(openCloseSignal: Boolean) {
      super.onOpenCloseSignalSwitched(openCloseSignal)
      if (openCloseSignal) {
        sendEvent(Events.PrinterDrawerOpened.toString().replaceFirstChar { fc -> fc.lowercase() })
      } else {
        sendEvent(Events.PrinterDrawerClosed.toString().replaceFirstChar { fc -> fc.lowercase() })
      }
    }

    override fun onCommunicationError(e: StarIO10Exception) {
      super.onCommunicationError(e)
      sendEvent(Events.PrinterDrawerCommunicationError.toString().replaceFirstChar { fc -> fc.lowercase() }, e.message)
    }
  }
  private val inputDeviceDelegate = object : InputDeviceDelegate() {
    override fun onConnected() {
      super.onConnected()
      sendEvent(Events.InputDeviceConnected.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onDisconnected() {
      super.onDisconnected()
      sendEvent(Events.InputDeviceDisconnected.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onCommunicationError(e: StarIO10Exception) {
      super.onCommunicationError(e)
      sendEvent(Events.InputDeviceCommunicationError.toString().replaceFirstChar { fc -> fc.lowercase() }, e.message)
    }

    override fun onDataReceived(data: List<Byte>) {
      super.onDataReceived(data)
      sendEvent(Events.InputDeviceReadData.toString().replaceFirstChar { fc -> fc.lowercase() }, String(data.toByteArray()))
    }
  }
  private val displayDelegate = object : DisplayDelegate() {
    override fun onConnected() {
      super.onConnected()
      sendEvent(Events.DisplayConnected.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onDisconnected() {
      super.onDisconnected()
      sendEvent(Events.DisplayDisconnected.toString().replaceFirstChar { fc -> fc.lowercase() })
    }

    override fun onCommunicationError(e: StarIO10Exception) {
      super.onCommunicationError(e)
      sendEvent(Events.DisplayCommunicationError.toString().replaceFirstChar { fc -> fc.lowercase() }, e.message)
    }
  }
  override fun getName(): String {
    return "ReactNativeStarPrinter"
  }

  override fun invalidate() {
    super.invalidate()
    starPrinterCoroutineScope.cancel()
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @ReactMethod
  fun addListener(eventName: String) {
    listeners++
  }

  @Suppress("unused")
  @ReactMethod
  fun removeListeners(count: Int) {
    if (listeners > 1) {
      listeners -= count
    } else {
      listeners = 0
    }
  }

  private suspend fun maybeRequestBluetoothPermissions(): Int {
    val activity = reactContext.currentActivity as AppCompatActivity

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ContextCompat.checkSelfPermission(
          reactContext,
          Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        return PackageManager.PERMISSION_GRANTED
      } else {
        return suspendCoroutine { continuation ->
          val requestPermissionActivity = activity.activityResultRegistry.register(
            "CallWithPermission_${Manifest.permission.BLUETOOTH_CONNECT}",
            ActivityResultContracts.RequestPermission()
          ) { result ->
            if (result) {
              continuation.resume(PackageManager.PERMISSION_GRANTED)
            } else {
              continuation.resume(PackageManager.PERMISSION_DENIED)
            }
          }
          requestPermissionActivity.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
      }
    }
    return PackageManager.PERMISSION_GRANTED
  }

  @Suppress("unused")
  @ReactMethod
  fun searchPrinter(promise: Promise) {
    starPrinterCoroutineScope.launch {
      if (maybeRequestBluetoothPermissions() == PackageManager.PERMISSION_DENIED) {
        promise.reject("PRINTER_SEARCH_ERROR_NO_BLUETOOTH", "User did not grant permission to use device's bluetooth")
        return@launch
      }
      try {
        val interfaces = listOf(InterfaceType.Bluetooth, InterfaceType.Lan, InterfaceType.Usb/*, InterfaceType.Unknown*/)
        discoveryManager = StarDeviceDiscoveryManagerFactory.create(interfaces, reactApplicationContext.applicationContext)
        discoveryManager?.discoveryTime = 10000
        discoveryManager?.callback = object : StarDeviceDiscoveryManager.Callback {
          override fun onPrinterFound(printer: StarPrinter) {
            val printerDetails: WritableMap = WritableNativeMap()
            val connectionSettings: WritableMap = WritableNativeMap()
            val printerInfos: WritableMap = WritableNativeMap()

            connectionSettings.putString("identifier", printer.connectionSettings.identifier)
            connectionSettings.putString("interface", printer.connectionSettings.interfaceType.toString().lowercase())

            printerInfos.putString("model", printer.information?.model.toString())
            printerInfos.putString("emulation", printer.information?.emulation.toString())

            printerDetails.putMap("connection-settings", connectionSettings)
            printerDetails.putMap("information", printerInfos)
            promise.resolve(printerDetails)
          }

          override fun onDiscoveryFinished() {
            Log.i("react-native-star-printer", "Printer discovery finished")
          }
        }
        discoveryManager?.startDiscovery()
      } catch (e: StarIO10IllegalHostDeviceStateException) {
        if (e.errorCode == StarIO10ErrorCode.BluetoothUnavailable) {
          promise.reject("PRINTER_SEARCH_ERROR_NO_BLUETOOTH", e)
        } else {
          promise.reject("PRINTER_SEARCH_ERROR", e)
        }
      } catch (e: CancellationException) {
        discoveryManager?.stopDiscovery()
      } catch (e: Exception) {
        promise.reject("PRINTER_SEARCH_ERROR", e)
      }
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun connect(identifier: String, iFace: String = "bluetooth", promise: Promise) {
    starPrinterCoroutineScope.launch {
      if (maybeRequestBluetoothPermissions() == PackageManager.PERMISSION_DENIED) {
        promise.reject("PRINTER_CONNECT_BLUETOOTH_UNAVAILABLE", "User did not grant permission to use device's bluetooth")
        return@launch
      }
      val interfaceType = InterfaceType.values().firstOrNull{ it.toString() == iFace.trim().replaceFirstChar { fc -> fc.uppercase() }}

      if (interfaceType == null) {
        promise.reject("PRINTER_CONNECT_INVALID_INTERFACE_TYPE_PROVIDED", "$iFace isn't a valid interface type name")
        return@launch
      }
      val connectionSettings = StarConnectionSettings(interfaceType, identifier, true)

      try {
        starPrinter?.closeAsync()?.await()
        starPrinter = StarPrinter(connectionSettings, reactApplicationContext.applicationContext)
        starPrinter?.printerDelegate = printerDelegate
        starPrinter?.drawerDelegate = drawerDelegate
        starPrinter?.inputDeviceDelegate = inputDeviceDelegate
        starPrinter?.displayDelegate = displayDelegate
        starPrinter?.openTimeout = 10000
        starPrinter?.openAsync()?.await()
        promise.resolve(true)
      } catch (e: StarIO10InvalidOperationException) {
        promise.reject("PRINTER_OPEN_INVALID_OPERATION", e.message, e)
        starPrinter = null
      } catch (e: StarIO10CommunicationException) {
        promise.reject("PRINTER_OPEN_COMMUNICATION_ERROR", e.message, e)
        starPrinter = null
      } catch (e: StarIO10InUseException) {
        promise.reject("PRINTER_OPEN_PRINTER_IN_USE", e.message, e)
        starPrinter = null
      } catch (e: StarIO10NotFoundException) {
        promise.reject("PRINTER_OPEN_PRINTER_NOT_FOUND", e.message, e)
        starPrinter = null
      } catch (e: StarIO10ArgumentException) {
        promise.reject("PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID", e.message, e)
        starPrinter = null
      } catch (e: StarIO10BadResponseException) {
        promise.reject("PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER", e.message, e)
        starPrinter = null
      } catch (e: StarIO10IllegalHostDeviceStateException) {
        when (e.errorCode) {
          StarIO10ErrorCode.NetworkUnavailable -> promise.reject("PRINTER_OPEN_NETWORK_UNAVAILABLE", e.message, e)
          StarIO10ErrorCode.BluetoothUnavailable -> promise.reject("PRINTER_OPEN_BLUETOOTH_UNAVAILABLE", e.message, e)
          StarIO10ErrorCode.UsbUnavailable -> promise.reject("PRINTER_OPEN_USB_UNAVAILABLE", e.message, e)
          else -> promise.reject("PRINTER_OPEN_ILLEGAL_DEVICE_STATE", e.message, e)
        }
        starPrinter = null
      } catch (e: StarIO10UnknownException) {
        promise.reject("PRINTER_OPEN_UNKNOWN_ERROR", "An unknown error happened while connecting to the printer $identifier using $iFace", e)
        starPrinter = null
      } catch (e: Exception) {
        promise.reject("PRINTER_OPEN_UNKNOWN_ERROR", "An unknown error happened while connecting to the printer $identifier using $iFace", e)
        starPrinter = null
      }
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun disconnect(promise: Promise) {
    starPrinterCoroutineScope.launch {
      starPrinter?.let { printer ->
        printer.closeAsync().await()
        starPrinter = null
        promise.resolve(true)
      } ?: run {
        promise.resolve(true)
      }
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun getStatus(promise: Promise) {
    starPrinter?.let {printer ->
      starPrinterCoroutineScope.launch {
        try {
          val status = printer.getStatusAsync().await()
          val printerStatus = WritableNativeMap()

          printerStatus.putBoolean("hasError", status.hasError)
          printerStatus.putBoolean("coverOpen", status.coverOpen)
          printerStatus.putBoolean("drawerOpenCloseSignal", status.drawerOpenCloseSignal)
          printerStatus.putBoolean("paperEmpty", status.paperEmpty)
          printerStatus.putBoolean("paperNearEmpty", status.paperNearEmpty)

          printerStatus.putBoolean("cutterError", status.detail.cutterError ?: false)
          printerStatus.putBoolean("paperSeparatorError", status.detail.paperSeparatorError ?: false)
          printerStatus.putBoolean("paperJamError", status.detail.paperJamError ?: false)
          printerStatus.putBoolean("paperPresent", status.detail.paperPresent ?: false)
          printerStatus.putBoolean("drawerOpenError", status.detail.drawerOpenError ?: false)
          printerStatus.putBoolean("printUnitOpen", status.detail.printUnitOpen ?: false)
          printerStatus.putInt("detectedPaperWidth", status.detail.detectedPaperWidth ?: -1)

          promise.resolve(printerStatus)
        } catch (e: StarIO10InvalidOperationException) {
          promise.reject("PRINTER_GET_STATUS_INVALID_OPERATION", "Not connected to any printer", e)
        } catch (e: StarIO10CommunicationException) {
          promise.reject("PRINTER_GET_STATUS_COMMUNICATION_ERROR", "Failed to contact the printer printer", e)
        } catch (e: StarIO10BadResponseException) {
          promise.reject("PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER", "Failed to contact the printer printer", e)
        }
      }
    } ?: run {
      promise.reject("PRINTER_GET_STATUS_NO_PRINTER_CONNECTION", "Not connected to any printer")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun print(commands: ReadableArray, internationalCharacterType: String?, promise: Promise) {
    starPrinterCoroutineScope.launch {
      starPrinter?.let {printer ->
        val commandBuilder = StarXpandCommandBuilder()
        val documentBuilder = DocumentBuilder()
        val printerBuilder = PrinterBuilder()
        val bitmaps = mutableListOf<Bitmap>()

        try {
          val printCommands = commands.toArrayList().filterIsInstance<Map<String, Any?>>().map { command -> printerCommandFactory(command) }
          printerBuilder.styleInternationalCharacter(
            printerInternationalCharacterTypeFromString(
              internationalCharacterType
            )
          )

          for (command in printCommands) {
            if (command is Print) {
              command.style?.align?.let { printerBuilder.styleAlignment(it) }
              command.style?.bold?.let { printerBuilder.styleBold(it) }
              printerBuilder.styleMagnification(
                MagnificationParameter(
                  (command.style?.widthExpansion ?: 0) + 1,
                  (command.style?.heightExpansion ?: 0) + 1,
                )
              )
              when (command.type) {
                PrintDataType.TEXT -> printerBuilder.actionPrintText(command.data)
                PrintDataType.BARCODE -> {
                  val barcodeParams = BarcodeParameter(command.data, BarcodeSymbology.Code128)
                  command.style?.barWidth?.let { barsWidth -> barcodeParams.setBarDots(barsWidth) }
                  printerBuilder.actionPrintBarcode(barcodeParams)
                }

                PrintDataType.IMAGE -> {
                  downloadImage(command.data)?.let { image ->
                    val imageWidth = command.style?.width ?: async {
                      try {
                        val printerStatus = printer.getStatusAsync().await()
                        printerStatus.detail.detectedPaperWidth
                      } catch (e: Exception) {
                        Log.e(
                          "react-native-star-printer",
                          "Exception while trying to detect paper width ${e.message}"
                        )
                        null
                      }
                    }.await() ?: image.width
                    bitmaps + image
                    printerBuilder.actionPrintImage(ImageParameter(image, imageWidth))
                  }
                }
              }
            }
            // We're not going to printer anything but tell the printer to perform an action instead
            if (command is PrinterAction) {
              // This is a cut paper action
              if (
                listOf(
                  Action.CUT,
                  Action.PARTIAL_CUT,
                  Action.PARTIAL_DIRECT,
                  Action.FULL_DIRECT
                ).contains(command.action)
              ) {
                printerBuilder.actionCut(Action.toCutType(command.action))
              } else if (command.action === Action.PAPER_FEED) {// This is a feed paper action
                val feedHeight = command.args?.let { it["height"] as? Double } ?: 1.0

                printerBuilder.actionFeed(feedHeight)
              } else if (command.action === Action.FEED_LINE) {// This is a feed line action
                printerBuilder.actionFeedLine(1)
              } else if (command.action === Action.PRINT_RULED_LINE) {// This is a feed line action
                val ruledLineWidth = command.args?.let { it["width"] as? Double } ?: 48.0
                val ruledLineParameters = RuledLineParameter(ruledLineWidth)

                command.args?.let { it["thickness"] as? Double }?.let { thickness -> ruledLineParameters.setThickness(thickness) }
                command.args?.let { it["xOffset"] as? Double }?.let { offset -> ruledLineParameters.setX(offset) }
                command.args?.let { it["style"] as? String }?.let { style -> {
                  if (style.lowercase() == LineStyle.Double.toString().lowercase()) {
                    ruledLineParameters.setLineStyle(LineStyle.Double)
                  }
                  if (style.lowercase() == LineStyle.Single.toString().lowercase()) {
                    ruledLineParameters.setLineStyle(LineStyle.Single)
                  }
                } }
                printerBuilder.styleAlignment(Alignment.Center)
                printerBuilder.actionPrintRuledLine(ruledLineParameters)
              }
            }
          }
          commandBuilder.addDocument(documentBuilder.addPrinter(printerBuilder))
          printer.printAsync(commandBuilder.getCommands()).await()
          promise.resolve(true)
        } catch (e: StarIO10InvalidOperationException) {
          promise.reject("PRINTER_PRINT_INVALID_OPERATION", e.message, e)
        } catch (e: StarIO10CommunicationException) {
          promise.reject("PRINTER_PRINT_COMMUNICATION_ERROR", e.message, e)
        } catch (e: StarIO10UnprintableException) {
          when (e.errorCode) {
            StarIO10ErrorCode.DeviceHasError -> promise.reject("PRINTER_PRINT_DEVICE_ERROR", e.message, e)
            StarIO10ErrorCode.PrinterHoldingPaper -> promise.reject("PRINTER_PRINT_PRINTER_HOLDING_PAPER", e.message, e)
            StarIO10ErrorCode.PrintingTimeout -> promise.reject("PRINTER_PRINT_PRINTING_TIMEOUT", e.message, e)
            else -> promise.reject("PRINTER_PRINT_INVALID_DEVICE_STATUS", e.message, e)
          }
        } catch (e: StarIO10BadResponseException) {
          promise.reject("PRINTER_PRINT_INVALID_RESPONSE_FROM_DEVICE", e.message, e)
        } catch (e: StarIO10UnknownException) {
          promise.reject("PRINTER_PRINT_UNKNOWN_ERROR", e.message, e)
        } catch (e: Exception) {
          promise.reject("PRINTER_PRINT_UNKNOWN_ERROR", "Error while printing ${e.message}", e)
        } finally {
          // Memory clean up
          bitmaps.forEach { it.recycle() }
          bitmaps.map { null }
          bitmaps.clear()
        }
      } ?: run {
        promise.reject("PRINTER_PRINT_NO_PRINTER_CONNECTION", "Not connected to any printer")
      }
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun openCashDrawer(promise: Promise) {
    starPrinter?.let {printer ->
      val commandBuilder = StarXpandCommandBuilder()
      val drawerBuilder = DrawerBuilder()
      val documentBuilder = DocumentBuilder()

      starPrinterCoroutineScope.launch {
        try {
          drawerBuilder.actionOpen(OpenParameter().setChannel(Channel.No1))
          printer.printAsync(commandBuilder.addDocument(documentBuilder.addDrawer(drawerBuilder)).getCommands()).await()
          promise.resolve(true)
        } catch (e: StarIO10InvalidOperationException) {
          promise.reject("PRINTER_OPEN_DRAWER_INVALID_OPERATION", e.message, e)
        } catch (e: StarIO10CommunicationException) {
          promise.reject("PRINTER_OPEN_DRAWER_COMMUNICATION_ERROR", e.message, e)
        } catch (e: StarIO10UnprintableException) {
          when (e.errorCode) {
            StarIO10ErrorCode.DeviceHasError -> promise.reject("PRINTER_OPEN_DRAWER_DEVICE_ERROR", e.message, e)
            StarIO10ErrorCode.PrinterHoldingPaper -> promise.reject("PRINTER_OPEN_DRAWER_PRINTER_HOLDING_PAPER", e.message, e)
            StarIO10ErrorCode.PrintingTimeout -> promise.reject("PRINTER_OPEN_DRAWER_PRINTING_TIMEOUT", e.message, e)
            else -> promise.reject("PRINTER_OPEN_DRAWER_INVALID_DEVICE_STATUS", e.message, e)
          }
        } catch (e: StarIO10BadResponseException) {
          promise.reject("PRINTER_OPEN_DRAWER_INVALID_RESPONSE_FROM_DEVICE", e.message, e)
        } catch (e: StarIO10UnknownException) {
          promise.reject("PRINTER_OPEN_DRAWER_UNKNOWN_ERROR", e.message, e)
        } catch (e: Exception) {
          promise.reject("PRINTER_OPEN_DRAWER_UNKNOWN_ERROR", "Error while printing ${e.message}", e)
        }
      }
    } ?: run {
      promise.reject("PRINTER_OPEN_DRAWER_NO_PRINTER_CONNECTION", "Not connected to any printer")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun showTextOnDisplay(content: String, backlight: Boolean?, contrast: Double?, cursorState: String?, charset: String?, promise: Promise) {
    starPrinter?.let {printer ->
      val commandBuilder = StarXpandCommandBuilder()
      val displayBuilder = DisplayBuilder()
      val documentBuilder = DocumentBuilder()

      starPrinterCoroutineScope.launch {
        try {
          displayBuilder.styleInternationalCharacter(displayInternationalCharacterTypeFromString(charset ?: "usa"))
          displayBuilder
            .actionSetBackLightState(backlight ?: true)
            .actionSetContrast(getDisplayContrast(contrast ?: .0))
            .actionSetCursorState(cursorStateFromString(cursorState ?: "off"))
            .actionShowText(content)
          printer.printAsync(commandBuilder.addDocument(documentBuilder.addDisplay(displayBuilder)).getCommands()).await()
          promise.resolve(true)
        } catch (e: StarIO10InvalidOperationException) {
          promise.reject("DISPLAY_SHOW_TEXT_INVALID_OPERATION", e.message, e)
        } catch (e: StarIO10CommunicationException) {
          promise.reject("DISPLAY_SHOW_TEXT_COMMUNICATION_ERROR", e.message, e)
        } catch (e: StarIO10UnprintableException) {
          when (e.errorCode) {
            StarIO10ErrorCode.DeviceHasError -> promise.reject("DISPLAY_SHOW_TEXT_DEVICE_ERROR", e.message, e)
            StarIO10ErrorCode.PrinterHoldingPaper -> promise.reject("DISPLAY_SHOW_TEXT_PRINTER_HOLDING_PAPER", e.message, e)
            StarIO10ErrorCode.PrintingTimeout -> promise.reject("DISPLAY_SHOW_TEXT_PRINTING_TIMEOUT", e.message, e)
            else -> promise.reject("DISPLAY_SHOW_TEXT_INVALID_DEVICE_STATUS", e.message, e)
          }
        } catch (e: StarIO10BadResponseException) {
          promise.reject("DISPLAY_SHOW_TEXT_INVALID_RESPONSE_FROM_DEVICE", e.message, e)
        } catch (e: StarIO10UnknownException) {
          promise.reject("DISPLAY_SHOW_TEXT_UNKNOWN_ERROR", e.message, e)
        } catch (e: Exception) {
          promise.reject("DISPLAY_SHOW_TEXT_UNKNOWN_ERROR", "Error while printing ${e.message}", e)
        }
      }
    } ?: run {
      promise.reject("DISPLAY_SHOW_TEXT_NO_PRINTER_CONNECTION", "Not connected to any printer")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun clearDisplay(promise: Promise) {
    starPrinter?.let {printer ->
      val commandBuilder = StarXpandCommandBuilder()
      val displayBuilder = DisplayBuilder()
      val documentBuilder = DocumentBuilder()

      starPrinterCoroutineScope.launch {
        try {
          displayBuilder.actionClearAll()
          printer.printAsync(commandBuilder.addDocument(documentBuilder.addDisplay(displayBuilder)).getCommands()).await()
          promise.resolve(true)
        } catch (e: StarIO10InvalidOperationException) {
          promise.reject("DISPLAY_CLEAR_INVALID_OPERATION", e.message, e)
        } catch (e: StarIO10CommunicationException) {
          promise.reject("DISPLAY_CLEAR_COMMUNICATION_ERROR", e.message, e)
        } catch (e: StarIO10UnprintableException) {
          when (e.errorCode) {
            StarIO10ErrorCode.DeviceHasError -> promise.reject("DISPLAY_CLEAR_DEVICE_ERROR", e.message, e)
            StarIO10ErrorCode.PrinterHoldingPaper -> promise.reject("DISPLAY_CLEAR_PRINTER_HOLDING_PAPER", e.message, e)
            StarIO10ErrorCode.PrintingTimeout -> promise.reject("DISPLAY_CLEAR_PRINTING_TIMEOUT", e.message, e)
            else -> promise.reject("DISPLAY_CLEAR_INVALID_DEVICE_STATUS", e.message, e)
          }
        } catch (e: StarIO10BadResponseException) {
          promise.reject("DISPLAY_CLEAR_INVALID_RESPONSE_FROM_DEVICE", e.message, e)
        } catch (e: StarIO10UnknownException) {
          promise.reject("DISPLAY_CLEAR_UNKNOWN_ERROR", e.message, e)
        } catch (e: Exception) {
          promise.reject("DISPLAY_CLEAR_UNKNOWN_ERROR", "Error while printing ${e.message}", e)
        }
      }
    } ?: run {
      promise.reject("DISPLAY_CLEAR_NO_PRINTER_CONNECTION", "Not connected to any printer")
    }
  }

  private suspend fun downloadImage(url: String): Bitmap? {
    return withContext(Dispatchers.IO) {
      try {
        BitmapFactory.decodeStream(URL(url).openStream())
      } catch (e: IOException) {
        Log.e("react-native-star-printer", "Could not open stream for image at URL $url")
        null
      } catch (e: MalformedURLException) {
        Log.e("react-native-star-printer", "Image URL is malformed $url")
        null
      } catch (e: Exception) {
        Log.e("react-native-star-printer", "Error while downloading image $url into a bitmap")
        null
      }
    }
  }

  private fun sendEvent(name: String, body: String? = null) {
    Log.d("react-native-star-printer", "Send event, listeners count $listeners")
    if (listeners > 0) {
      val reactContext: ReactContext = reactApplicationContext
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(name, body)
    }
  }
}
