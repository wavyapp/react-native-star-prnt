package fr.wavyapp.reactNativeStarPrinter

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.starmicronics.stario.PortInfo
import com.starmicronics.stario.StarBluetoothManager
import com.starmicronics.stario.StarIOPort
import com.starmicronics.stario.StarIOPortException
import com.starmicronics.stario.StarPrinterStatus
import com.starmicronics.starioextension.ConnectionCallback
import com.starmicronics.starioextension.ICommandBuilder
import com.starmicronics.starioextension.ICommandBuilder.AlignmentPosition
import com.starmicronics.starioextension.ICommandBuilder.BarcodeSymbology
import com.starmicronics.starioextension.ICommandBuilder.BarcodeWidth
import com.starmicronics.starioextension.ICommandBuilder.BitmapConverterRotation
import com.starmicronics.starioextension.ICommandBuilder.BlackMarkType
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction
import com.starmicronics.starioextension.ICommandBuilder.FontStyleType
import com.starmicronics.starioextension.ICommandBuilder.LogoSize
import com.starmicronics.starioextension.ICommandBuilder.PeripheralChannel
import com.starmicronics.starioextension.ICommandBuilder.QrCodeLevel
import com.starmicronics.starioextension.ICommandBuilder.QrCodeModel
import com.starmicronics.starioextension.IConnectionCallback
import com.starmicronics.starioextension.IConnectionCallback.ConnectResult
import com.starmicronics.starioextension.IDisplayCommandBuilder
import com.starmicronics.starioextension.StarBluetoothManagerFactory
import com.starmicronics.starioextension.StarIoExt
import com.starmicronics.starioextension.StarIoExt.Emulation
import com.starmicronics.starioextension.StarIoExtManager
import com.starmicronics.starioextension.StarIoExtManagerListener
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.Collections
import java.util.function.Consumer
import java.util.concurrent.Callable

internal class ThreadedPromises(promise: Promise) {
  private var promises: MutableCollection<Promise> = Collections.synchronizedCollection(ArrayList<Promise>())

  init {
    promises.add(promise)
  }

  fun addPromise(promise: Promise) {
    promises.add(promise)
  }

  fun resolveAll(result: ArrayList<HashMap<String?, String?>>) {
    if (promises.isNotEmpty()) {
      promises.forEach(Consumer { promise: Promise ->
        val ports: WritableArray = WritableNativeArray()
        for (port in result) {
          val portInfo: WritableMap = WritableNativeMap()
          for (property in port.keys) {
            portInfo.putString(property!!, port[property])
          }
          ports.pushMap(portInfo)
        }
        promise.resolve(ports)
      })
      promises.clear()
    }
  }

  fun rejectAll(message: String?, exception: Exception?) {
    if (promises.isNotEmpty()) {
      promises.forEach(Consumer { promise: Promise -> promise.reject(message, exception) })
      promises.clear()
    }
  }
}

class ReactStarPrinterModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(
    reactContext
  ) {
  private var starIoExtManager: StarIoExtManager? = null
  private var mBluetoothManager: StarBluetoothManager? = null
  private var mAutoConnect = false
  private val starIoExtManagerListener: StarIoExtManagerListener =
    object : StarIoExtManagerListener() {
      override fun onPrinterImpossible() {
        sendEvent("printerImpossible", null)
      }

      override fun onPrinterOnline() {
        sendEvent("printerOnline", null)
      }

      override fun onPrinterOffline() {
        sendEvent("printerOffline", null)
      }

      override fun onPrinterPaperReady() {
        sendEvent("printerPaperReady", null)
      }

      override fun onPrinterPaperNearEmpty() {
        sendEvent("printerPaperNearEmpty", null)
      }

      override fun onPrinterPaperEmpty() {
        sendEvent("printerPaperEmpty", null)
      }

      override fun onPrinterCoverOpen() {
        sendEvent("printerCoverOpen", null)
      }

      override fun onPrinterCoverClose() {
        sendEvent("printerCoverClose", null)
      }

      //Cash Drawer events
      override fun onCashDrawerOpen() {
        sendEvent("cashDrawerOpen", null)
      }

      override fun onCashDrawerClose() {
        sendEvent("cashDrawerClose", null)
      }

      override fun onBarcodeReaderImpossible() {
        sendEvent("barcodeReaderImpossible", null)
      }

      override fun onBarcodeReaderConnect() {
        sendEvent("barcodeReaderConnect", null)
      }

      override fun onBarcodeReaderDisconnect() {
        sendEvent("barcodeReaderDisconnect", null)
      }

      override fun onBarcodeDataReceive(data: ByteArray) {
        sendEvent("barcodeReaderDataReceive", String(data))
      }
    }
  private val portDiscoveryThreadPool = LinkedHashMap<String, ThreadedPromises>()
  private var listeners = 0

  override fun getName(): String {
    return "ReactNativeStarPrinter"
  }

  private fun threadedPortDiscovery(strInterface: String): Thread {
    return Thread {
      val currentThreadedPromises = portDiscoveryThreadPool[strInterface]

      try {
        when (strInterface) {
          "LAN" -> currentThreadedPromises!!.resolveAll(getPortDiscovery("LAN"))
          "Bluetooth" -> {
            val onGranted = Callable { getPortDiscovery("Bluetooth") }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              val callPermissionWrapper = CallWithPermission(
                (reactContext.currentActivity as AppCompatActivity?)!!,
                Manifest.permission.BLUETOOTH_CONNECT
              )
              currentThreadedPromises!!.resolveAll(
                callPermissionWrapper.callMayThrow(
                  { getPortDiscovery("Bluetooth") }
                ) { currentThreadedPromises.rejectAll("BLUETOOTH_PERMISSION_DENIED", null) }!!
              )
            } else {
              onGranted.call()
            }
          }
          "USB" -> currentThreadedPromises!!.resolveAll(getPortDiscovery("USB"))
          else -> currentThreadedPromises!!.resolveAll(getPortDiscovery("All"))
        }
        portDiscoveryThreadPool.remove(strInterface)
      } catch (exception: Exception) {
        currentThreadedPromises!!.rejectAll("PORT_DISCOVERY_ERROR", exception)
      }
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @ReactMethod
  fun addListener(eventName: String) {
    listeners++
  }

  @Suppress("unused")
  @ReactMethod
  fun removeListeners(count: Int) {
    listeners -= count
  }

  @Suppress("unused")
  @ReactMethod
  fun portDiscovery(strInterface: String, promise: Promise) {
    val currentThreadedPromises = portDiscoveryThreadPool[strInterface]
    if (currentThreadedPromises == null) {
      val newThread = threadedPortDiscovery(strInterface)
      portDiscoveryThreadPool[strInterface] = ThreadedPromises(promise)
      newThread.start()
    } else {
      currentThreadedPromises.addPromise(promise)
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun checkStatus(portName: String?, emulation: String?, promise: Promise) {
    Log.d("react-native-star-printer", "Checking  printer status")
    Thread {
      val onGranted = Callable<Any?> {
        val portSettings = getPortSettingsOption(emulation)
        Log.d("react-native-star-printer", "Android bluetooth scan permission granted")
        var port: StarIOPort? = null
        try {
          port = StarIOPort.getPort(portName, portSettings, 10000, reactApplicationContext)
          Log.d("react-native-star-printer", String.format("Got star IO port %s", port.portName))

          // A sleep is used to get time for the socket to completely open
          try {
            Thread.sleep(500)
          } catch (e: InterruptedException) {
            // Do nothing
          }
          Log.d("react-native-star-printer", "End sleep")
          val firmwareInformationMap = port.firmwareInformation
          val status: StarPrinterStatus = port.retreiveStatus()
          Log.d("react-native-star-printer", "Retrieved printer status")
          Log.d("react-native-star-printer", String.format("Printer is offline %b", status.offline))
          Log.d(
            "react-native-star-printer",
            String.format("Printer cover is open %b", status.coverOpen)
          )
          Log.d("react-native-star-printer", String.format("Paper is present %b", status.paperPresent))
          Log.d(
            "react-native-star-printer",
            String.format("Paper detection error %b", status.paperDetectionError)
          )
          Log.d(
            "react-native-star-printer",
            String.format("Presenter paper is present %b", status.presenterPaperPresent)
          )
          Log.d(
            "react-native-star-printer",
            String.format("Receipt paper empty %b", status.receiptPaperEmpty)
          )
          Log.d(
            "react-native-star-printer",
            String.format("Peeler paper is present %b", status.peelerPaperPresent)
          )
          Log.d(
            "react-native-star-printer",
            String.format("Printer has unrecoverable error %b", status.unrecoverableError)
          )
          Log.d("react-native-star-printer", firmwareInformationMap["ModelName"]!!)
          Log.d("react-native-star-printer", firmwareInformationMap["FirmwareVersion"]!!)
          val json = WritableNativeMap()
          json.putBoolean("offline", status.offline)
          json.putBoolean("coverOpen", status.coverOpen)
          json.putBoolean("cutterError", status.cutterError)
          json.putBoolean("receiptPaperEmpty", status.receiptPaperEmpty)
          json.putString("ModelName", firmwareInformationMap["ModelName"])
          json.putString("FirmwareVersion", firmwareInformationMap["FirmwareVersion"])
          json.putString("autoConnect", mBluetoothManager!!.autoConnect.toString())
          promise.resolve(json)
        } catch (e: StarIOPortException) {
          promise.reject("CHECK_STATUS_ERROR", e)
          Log.d("react-native-star-printer", e.message!!)
        } finally {
          if (port != null) {
            try {
              StarIOPort.releasePort(port)
            } catch (e: StarIOPortException) {
              promise.reject("CHECK_STATUS_ERROR", e.message)
            }
          }
        }
        null
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val callPermissionWrapper = CallWithPermission(
            (reactContext.currentActivity as AppCompatActivity?)!!,
            Manifest.permission.BLUETOOTH_SCAN
          )
          callPermissionWrapper.callMayThrow(onGranted) {
            promise.reject(
              "CHECK_STATUS_ERROR",
              "Bluetooth scan permission was denied by the user"
            )
          }
        } else {
          onGranted.call()
        }
      } catch (e: Exception) {
        promise.reject("CHECK_STATUS_ERROR", e.message)
      }
    }.start()
  }

  @Suppress("unused")
  @ReactMethod
  fun connect(portName: String?, emulation: String?, hasBarcodeReader: Boolean, promise: Promise) {
    val context: Context? = currentActivity
    val portSettings = getPortSettingsOption(emulation)
    if (starIoExtManager != null && starIoExtManager!!.port != null) {
      starIoExtManager!!.disconnect(object : IConnectionCallback {
        override fun onConnected(connectResult: ConnectResult) {}
        override fun onDisconnected() {}
      })
    }
    starIoExtManager = StarIoExtManager(
      if (hasBarcodeReader) StarIoExtManager.Type.WithBarcodeReader else StarIoExtManager.Type.Standard,
      portName,
      portSettings,
      10000,
      context
    )
    starIoExtManager!!.setListener(starIoExtManagerListener)
    try {
      mBluetoothManager = StarBluetoothManagerFactory.getManager(
        portName,
        portSettings,
        10000,
        getEmulation(emulation)
      )
    } catch (e: StarIOPortException) {
      promise.reject("CONNECT_ERROR", "Error Connecting to the printer", e)
    }
    Thread {
      if (starIoExtManager != null) {
        try {
          starIoExtManager!!.connect(object : ConnectionCallback() {
            override fun onConnected(connected: Boolean, resultCode: Int) {
              Log.d(
                "react-native-star-printer",
                String.format("Connection result %b code %d", connected, resultCode)
              )
              if (connected) {
                promise.resolve("Printer Connected")
              } else {
                promise.reject("CONNECT_ERROR", "Error Connecting to the printer")
              }
            }

            override fun onDisconnected() {
              Log.d("react-native-star-printer", "Printer disconnected")
            }
          })
        } catch (e: NullPointerException) {
          promise.reject("CONNECT_ERROR", "Error Connecting to the printer", e)
        }
      }
      promise.reject("CONNECT_ERROR", "Error Connecting to the printer")
    }.start()
  }

  @Suppress("unused")
  @ReactMethod
  fun disconnect(promise: Promise) {
    Thread {
      if (starIoExtManager != null && starIoExtManager!!.port != null) {
        starIoExtManager!!.disconnect(object : IConnectionCallback {
          override fun onConnected(connectResult: ConnectResult) {
            // nothing
          }

          override fun onDisconnected() {
            sendEvent("printerOffline", null)
            starIoExtManager!!.setListener(null) //remove the listener?
            promise.resolve("Printer Disconnected")
          }
        })
      } else {
        promise.resolve("No printers connected")
      }
    }.start()
  }

  @Suppress("unused")
  @ReactMethod
  fun showPriceIndicator(
    portName: String?,
    emulation: String?,
    displayCommands: ReadableArray,
    promise: Promise
  ) {
    if (starIoExtManager != null) {
      val portSettings = getPortSettingsOption(emulation)
      val context: Context? = currentActivity
      Thread {
        val builder = StarIoExt.createDisplayCommandBuilder(StarIoExt.DisplayModel.SCD222)
        builder.appendClearScreen()
        builder.appendCursorMode(IDisplayCommandBuilder.CursorMode.Off)
        builder.appendHomePosition()
        builder.appendInternational(IDisplayCommandBuilder.InternationalType.France)
        builder.appendCodePage(IDisplayCommandBuilder.CodePageType.CP1252)
        for (i in 0 until displayCommands.size()) {
          val command = displayCommands.getMap(i)
          try {
            builder.append(
              command.getString("appendCustomerDisplay")!!.toByteArray(charset("UTF-8"))
            )
          } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
          }
        }
        val commands = builder.passThroughCommands
        portName?.let {  //use StarIOPort
          sendCommand(context, it, portSettings, commands, promise)
        }
          ?: sendCommandsDoNotCheckCondition(commands, starIoExtManager!!.port, promise)
      }.start()
    } else {
      promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun cleanCustomerDisplay(
    portName: String?,
    emulation: String?,
    displayCommands: ReadableArray,
    promise: Promise
  ) {
    if (starIoExtManager != null) {
      val portSettings = getPortSettingsOption(emulation)
      val context: Context? = currentActivity
      Thread {
        val builder = StarIoExt.createDisplayCommandBuilder(StarIoExt.DisplayModel.SCD222)
        for (i in 0 until displayCommands.size()) {
          val command = displayCommands.getMap(i)
          val isCleaning = command.getInt("cleanCustomerDisplay")
          if (isCleaning == 1) {
            builder.appendClearScreen()
          }
        }
        val commands = builder.passThroughCommands
        portName?.let { sendCommand(context, it, portSettings, commands, promise) }
          ?: sendCommandsDoNotCheckCondition(commands, starIoExtManager!!.port, promise)
      }.start()
    } else {
      promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun turnCustomerDisplay(
    turnTo: String,
    portName: String?,
    emulation: String?,
    promise: Promise
  ) {
    if (turnTo != "on" && turnTo != "off") {
      promise.reject("STARIO_PORT_EXCEPTION", "Bad turnTo parameter")
    }
    if (starIoExtManager != null) {
      val portSettings = getPortSettingsOption(emulation)
      val context: Context? = currentActivity
      Thread {
        val builder = StarIoExt.createDisplayCommandBuilder(StarIoExt.DisplayModel.SCD222)
        if (turnTo == "on") {
          builder.appendTurnOn(true)
        } else {
          builder.appendTurnOn(false)
        }
        val commands = builder.passThroughCommands
        portName?.let { sendCommand(context, it, portSettings, commands, promise) }
          ?: sendCommandsDoNotCheckCondition(commands, starIoExtManager!!.port, promise)
      }.start()
    } else {
      promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun print(portName: String?, emulation: String?, printCommands: ReadableArray, promise: Promise) {
    if (starIoExtManager != null) {
      val portSettings = getPortSettingsOption(emulation)
      val context: Context? = currentActivity
      Thread {
        val builder = StarIoExt.createCommandBuilder(getEmulation(emulation))
        builder.beginDocument()
        appendCommands(builder, printCommands, context)
        builder.endDocument()
        val commands = builder.commands

        // use StarIOExtManager
        portName?.let {  //use StarIOPort
          sendCommand(context, it, portSettings, commands, promise)
        }
          ?: sendCommand(commands, starIoExtManager!!.port, promise)
      }.start()
    } else {
      promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun optimisticPrint(
    portName: String?,
    emulation: String?,
    printCommands: ReadableArray,
    promise: Promise
  ) {
    if (starIoExtManager != null) {
      val portSettings = getPortSettingsOption(emulation)
      val context: Context? = currentActivity
      Thread {
        val builder = StarIoExt.createCommandBuilder(getEmulation(emulation))
        builder.beginDocument()
        appendCommands(builder, printCommands, context)
        builder.endDocument()
        val commands = builder.commands
        portName?.let { sendCommand(context, it, portSettings, commands, promise) }
          ?: sendCommandsDoNotCheckCondition(commands, starIoExtManager!!.port, promise)
      }.start()
    } else {
      promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun setAutoConnect(autoConnectEnabled: Boolean, promise: Promise) {
    if (mBluetoothManager != null && mBluetoothManager!!.autoConnectCapability == StarBluetoothManager.StarBluetoothSettingCapability.SUPPORT) {
      mBluetoothManager!!.autoConnect = autoConnectEnabled
      mAutoConnect = mBluetoothManager!!.autoConnect
      promise.resolve(mAutoConnect)
    } else {
      promise.resolve("nosupport")
    }
  }

  private fun getEmulation(emulation: String?): Emulation {
    if (emulation == null) {
      return Emulation.StarPRNT
    }
    return if (emulation == "StarPRNT") Emulation.StarPRNT else if (emulation == "StarPRNTL") Emulation.StarPRNTL else if (emulation == "StarLine") Emulation.StarLine else if (emulation == "StarGraphic") Emulation.StarGraphic else if (emulation == "EscPos") Emulation.EscPos else if (emulation == "EscPosMobile") Emulation.EscPosMobile else if (emulation == "StarDotImpact") Emulation.StarDotImpact else Emulation.StarLine
  }

  @Suppress("LocalVariableName")
  @Throws(StarIOPortException::class)
  private fun getPortDiscovery(interfaceName: String): ArrayList<HashMap<String?, String?>> {
    val BTPortList: List<PortInfo>
    val TCPPortList: List<PortInfo>
    val USBPortList: List<PortInfo>?
    val arrayDiscovery = ArrayList<PortInfo>()
    Log.d("Get port discovery interface", interfaceName)
    if (interfaceName == "Bluetooth" || interfaceName == "All") {
      BTPortList = StarIOPort.searchPrinter("BT:")
      arrayDiscovery.addAll(BTPortList)
    }
    if (interfaceName == "LAN" || interfaceName == "All") {
      TCPPortList = StarIOPort.searchPrinter("TCP:")
      arrayDiscovery.addAll(TCPPortList)
    }
    if (interfaceName == "USB" || interfaceName == "All") {
      // On Android Q and up, we need permissions in order to access usb devices
      // this is a temporary fix while we work out to upgrade our dependency
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        USBPortList = try {
          StarIOPort.searchPrinter("USB:", reactApplicationContext)
        } catch (e: StarIOPortException) {
          ArrayList()
        }
        arrayDiscovery.addAll(USBPortList!!)
      }
    }
    val ports = ArrayList<HashMap<String?, String?>>()
    for (discovery in arrayDiscovery) {
      val portData = HashMap<String?, String?>()
      if (discovery.portName.startsWith("BT:")) {
        portData["portName"] = "BT:" + discovery.macAddress
      } else {
        portData["portName"] = discovery.portName
      }
      if (discovery.macAddress != "") {
        portData["macAddress"] = discovery.macAddress
        if (discovery.portName.startsWith("BT:")) {
          portData["modelName"] = discovery.portName
        } else if (discovery.modelName != "") {
          portData["modelName"] = discovery.modelName
        }
      } else if (interfaceName == "USB" || interfaceName == "All") {
        if (discovery.modelName != "") {
          portData["modelName"] = discovery.modelName
        }
        if (discovery.usbSerialNumber != " SN:") {
          portData["USBSerialNumber"] = discovery.usbSerialNumber
        }
      }
      ports.add(portData)
    }
    return ports
  }

  private fun getPortSettingsOption(emulation: String?): String { // generate the portsettings depending on the emulation type
    var portSettings = ""
    if (emulation == null) {
      return portSettings
    }
    when (emulation) {
      "EscPosMobile" -> portSettings += "mini"
      "EscPos" -> portSettings += "escpos"
      "StarPRNT", "StarPRNTL" -> {
        portSettings += "Portable"
        portSettings += ";l" //retry on
      }

      else -> portSettings += ""
    }
    return portSettings
  }

  @Suppress("unused")
  @ReactMethod
  fun optimisticSendCommand(
    commands: ByteArray,
    port: StarIOPort?,
    promise: Promise
  ) {
    try {
      /*
       * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
       */
      try {
        Thread.sleep(200)
      } catch (e: InterruptedException) {
        // Do nothing
      }
      if (port == null) { //Not connected or port closed
        promise.reject(
          "STARIO_PORT_EXCEPTION",
          "Unable to Open Port, Please Connect to the printer before sending commands"
        )
        return
      }
      var status: StarPrinterStatus = port.beginCheckedBlock()
      if (status.offline) {
        promise.reject("STARIO_PORT_EXCEPTION", "The printer is offline")
      }
      port.writePort(commands, 0, commands.size)
      port.setEndCheckedBlockTimeoutMillis(30000) // Change the timeout time of endCheckedBlock method.
      status = port.endCheckedBlock()
      if (status.coverOpen) {
        promise.reject("STARIO_PORT_EXCEPTION", "Cover open")
        sendEvent("printerCoverOpen", null)
        return
      } else if (status.receiptPaperEmpty) {
        promise.reject("STARIO_PORT_EXCEPTION", "Empty paper")
        sendEvent("printerPaperEmpty", null)
        return
      } else if (status.offline) {
        promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
        sendEvent("printerOffline", null)
        return
      }
      promise.resolve("Success!")
    } catch (e: StarIOPortException) {
      sendEvent("printerImpossible", e.message)
      promise.reject("STARIO_PORT_EXCEPTION", e.message)
      return
    }
  }

  private fun sendCommand(commands: ByteArray, port: StarIOPort?, promise: Promise): Boolean {
    try {
      /*
       * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
       */
      try {
        Thread.sleep(200)
      } catch (e: InterruptedException) {
        // Do nothing
      }
      if (port == null) { //Not connected or port closed
        promise.reject(
          "STARIO_PORT_EXCEPTION",
          "Unable to Open Port, Please Connect to the printer before sending commands"
        )
        return false
      }

      /*
       * Using Begin / End Checked Block method When sending large amounts of raster data,
       * adjust the value in the timeout in the "StarIOPort.getPort" in order to prevent
       * "timeout" of the "endCheckedBlock method" while a printing.
       *
       * If receipt print is success but timeout error occurs(Show message which is "There
       * was no response of the printer within the timeout period." ), need to change value
       * of timeout more longer in "StarIOPort.getPort" method.
       * (e.g.) 10000 -> 30000
       */
      var status: StarPrinterStatus = port.beginCheckedBlock()
      if (status.offline) {
        sendEvent("printerOffline", null)
        throw StarIOPortException("A printer is offline")
      }
      port.writePort(commands, 0, commands.size)
      port.setEndCheckedBlockTimeoutMillis(40000) // Change the timeout time of endCheckedBlock method.
      status = port.endCheckedBlock()
      if (status.coverOpen) {
        promise.reject("STARIO_PORT_EXCEPTION", "Cover open")
        sendEvent("printerCoverOpen", null)
        return false
      } else if (status.receiptPaperEmpty) {
        promise.reject("STARIO_PORT_EXCEPTION", "Empty paper")
        sendEvent("printerPaperEmpty", null)
        return false
      } else if (status.offline) {
        promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
        sendEvent("printerOffline", null)
        return false
      }
      promise.resolve("Success!")
    } catch (e: StarIOPortException) {
      sendEvent("printerImpossible", e.message)
      promise.reject("STARIO_PORT_EXCEPTION", e.message)
      return false
    }
    return true
  }

  @ReactMethod
  private fun sendCommandsDoNotCheckCondition(
    commands: ByteArray,
    port: StarIOPort?,
    promise: Promise
  ): Boolean {
    try {
      if (port == null) { //Not connected or port closed
        promise.reject(
          "STARIO_PORT_EXCEPTION",
          "Unable to Open Port, Please Connect to the printer before sending commands"
        )
        return false
      }
      port.writePort(commands, 0, commands.size)
      promise.resolve("Success!")
    } catch (e: StarIOPortException) {
      sendEvent("printerImpossible", e.message)
      promise.reject("STARIO_PORT_EXCEPTION", e.message)
      return false
    }
    return true
  }

  private fun sendCommand(
    context: Context?,
    portName: String,
    portSettings: String,
    commands: ByteArray,
    promise: Promise
  ): Boolean {
    var port: StarIOPort? = null
    try {
      /*
       * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
       */
      port = StarIOPort.getPort(portName, portSettings, 10000, context)
      try {
        Thread.sleep(100)
      } catch (e: InterruptedException) {
        // Do nothing
      }

      /*
       * Using Begin / End Checked Block method When sending large amounts of raster data,
       * adjust the value in the timeout in the "StarIOPort.getPort" in order to prevent
       * "timeout" of the "endCheckedBlock method" while a printing.
       *
       * If receipt print is success but timeout error occurs(Show message which is "There
       * was no response of the printer within the timeout period." ), need to change value
       * of timeout more longer in "StarIOPort.getPort" method.
       * (e.g.) 10000 -> 30000
       */
      var status = port.beginCheckedBlock()
      if (status.offline) {
        promise.reject("STARIO_PORT_EXCEPTION", "Cover open", StarIOPortException("A printer is offline"))
      }
      port.writePort(commands, 0, commands.size)
      port.setEndCheckedBlockTimeoutMillis(30000) // Change the timeout time of endCheckedBlock method.
      status = port.endCheckedBlock()
      if (status.coverOpen) {
        promise.reject("STARIO_PORT_EXCEPTION", "Cover open")
        return false
      } else if (status.receiptPaperEmpty) {
        promise.reject("STARIO_PORT_EXCEPTION", "Empty paper")
        return false
      } else if (status.offline) {
        promise.reject("STARIO_PORT_EXCEPTION", "Printer offline")
        return false
      }
      promise.resolve("Success!")
      return true
    } catch (e: StarIOPortException) {
      promise.reject("STARIO_PORT_EXCEPTION", e.message)
      return false
    } finally {
      if (port != null) {
        try {
          StarIOPort.releasePort(port)
        } catch (e: StarIOPortException) {
          // Do nothing
        }
      }
    }
  }

  private fun appendCommands(
    builder: ICommandBuilder,
    printCommands: ReadableArray,
    context: Context?
  ) {
    var encoding = Charset.forName("US-ASCII")
    for (i in 0 until printCommands.size()) {
      val command = printCommands.getMap(i)
      if (command.hasKey("appendCharacterSpace")) builder.appendCharacterSpace(command.getInt("appendCharacterSpace")) else if (command.hasKey(
          "appendEncoding"
        )
      ) encoding =
        getEncoding(command.getString("appendEncoding")) else if (command.hasKey("appendCodePage")) builder.appendCodePage(
        getCodePageType(command.getString("appendCodePage"))
      ) else if (command.hasKey("append")) builder.append(
        command.getString("append")!!.toByteArray(encoding!!)
      ) else if (command.hasKey("appendRaw")) builder.append(
        command.getString("appendRaw")!!.toByteArray(encoding!!)
      ) else if (command.hasKey("appendEmphasis")) builder.appendEmphasis(
        command.getString("appendEmphasis")!!.toByteArray(encoding!!)
      ) else if (command.hasKey("enableEmphasis")) builder.appendEmphasis(command.getBoolean("enableEmphasis")) else if (command.hasKey(
          "appendInvert"
        )
      ) builder.appendInvert(
        command.getString("appendInvert")!!.toByteArray(encoding!!)
      ) else if (command.hasKey("enableInvert")) builder.appendInvert(command.getBoolean("enableInvert")) else if (command.hasKey(
          "appendUnderline"
        )
      ) builder.appendUnderLine(
        command.getString("appendUnderline")!!.toByteArray(encoding!!)
      ) else if (command.hasKey("enableUnderline")) builder.appendUnderLine(command.getBoolean("enableUnderline")) else if (command.hasKey(
          "appendInternational"
        )
      ) builder.appendInternational(getInternational(command.getString("appendInternational"))) else if (command.hasKey(
          "appendLineFeed"
        )
      ) builder.appendLineFeed(command.getInt("appendLineFeed")) else if (command.hasKey("appendUnitFeed")) builder.appendUnitFeed(
        command.getInt("appendUnitFeed")
      ) else if (command.hasKey("appendLineSpace")) builder.appendLineSpace(command.getInt("appendLineSpace")) else if (command.hasKey(
          "appendFontStyle"
        )
      ) builder.appendFontStyle(getFontStyle(command.getString("appendFontStyle"))) else if (command.hasKey(
          "appendCutPaper"
        )
      ) builder.appendCutPaper(getCutPaperAction(command.getString("appendCutPaper"))) else if (command.hasKey(
          "openCashDrawer"
        )
      ) builder.appendPeripheral(getPeripheralChannel(command.getInt("openCashDrawer"))) else if (command.hasKey(
          "appendBlackMark"
        )
      ) builder.appendBlackMark(getBlackMarkType(command.getString("appendBlackMark"))) else if (command.hasKey(
          "appendBytes"
        )
      ) {
        val bytesArray = command.getArray("appendBytes")
        if (bytesArray != null) {
          val byteData = ByteArray(bytesArray.size() + 1)
          for (j in 0 until bytesArray.size()) {
            byteData[j] = bytesArray.getInt(j).toByte()
          }
          builder.append(byteData)
        }
      } else if (command.hasKey("appendRawBytes")) {
        val rawBytesArray = command.getArray("appendRawBytes")
        if (rawBytesArray != null) {
          val rawByteData = ByteArray(rawBytesArray.size() + 1)
          for (j in 0 until rawBytesArray.size()) {
            rawByteData[j] = rawBytesArray.getInt(j).toByte()
          }
          builder.appendRaw(rawByteData)
        }
      } else if (command.hasKey("appendAbsolutePosition")) {
        if (command.hasKey("data")) builder.appendAbsolutePosition(
          command.getString("data")!!
            .toByteArray(encoding!!), command.getInt("appendAbsolutePosition")
        ) else builder.appendAbsolutePosition(command.getInt("appendAbsolutePosition"))
      } else if (command.hasKey("appendAlignment")) {
        if (command.hasKey("data")) builder.appendAlignment(
          command.getString("data")!!
            .toByteArray(encoding!!), getAlignment(command.getString("appendAlignment"))
        ) else builder.appendAlignment(getAlignment(command.getString("appendAlignment")))
      } else if (command.hasKey("appendHorizontalTabPosition")) {
        val tabPositionsArray = command.getArray("appendHorizontalTabPosition")
        if (tabPositionsArray != null) {
          val tabPositions = IntArray(tabPositionsArray.size())
          for (j in 0 until tabPositionsArray.size()) {
            tabPositions[j] = tabPositionsArray.getInt(j)
          }
          builder.appendHorizontalTabPosition(tabPositions)
        }
      } else if (command.hasKey("appendLogo")) {
        val logoSize =
          if (command.hasKey("logoSize")) getLogoSize(command.getString("logoSize")) else getLogoSize(
            "Normal"
          )
        builder.appendLogo(logoSize, command.getInt("appendLogo"))
      } else if (command.hasKey("appendBarcode")) {
        val barcodeSymbology =
          if (command.hasKey("BarcodeSymbology")) getBarcodeSymbology(command.getString("BarcodeSymbology")) else getBarcodeSymbology(
            "Code128"
          )
        val barcodeWidth =
          if (command.hasKey("BarcodeWidth")) getBarcodeWidth(command.getString("BarcodeWidth")) else getBarcodeWidth(
            "Mode2"
          )
        val height = if (command.hasKey("height")) command.getInt("height") else 40
        val hri = !command.hasKey("hri") || command.getBoolean("hri")
        if (command.hasKey("absolutePosition")) {
          val position = command.getInt("absolutePosition")
          builder.appendBarcodeWithAbsolutePosition(
            command.getString("appendBarcode")!!.toByteArray(encoding!!),
            barcodeSymbology,
            barcodeWidth,
            height,
            hri,
            position
          )
        } else if (command.hasKey("alignment")) {
          val alignmentPosition = getAlignment(command.getString("alignment"))
          builder.appendBarcodeWithAlignment(
            command.getString("appendBarcode")!!.toByteArray(encoding!!),
            barcodeSymbology,
            barcodeWidth,
            height,
            hri,
            alignmentPosition
          )
        } else builder.appendBarcode(
          command.getString("appendBarcode")!!.toByteArray(encoding!!),
          barcodeSymbology,
          barcodeWidth,
          height,
          hri
        )
      } else if (command.hasKey("appendMultiple")) {
        val width = if (command.hasKey("width")) command.getInt("width") else 1
        val height = if (command.hasKey("height")) command.getInt("height") else 1
        builder.appendMultiple(
          command.getString("appendMultiple")!!.toByteArray(encoding!!), width, height
        )
      } else if (command.hasKey("enableMultiple")) {
        val width = if (command.hasKey("width")) command.getInt("width") else 1
        val height = if (command.hasKey("height")) command.getInt("height") else 1
        val enableMultiple = command.getBoolean("enableMultiple")
        if (enableMultiple) builder.appendMultiple(width, height) else builder.appendMultiple(
          1,
          1
        ) // Reset to default when false sent
      } else if (command.hasKey("appendQrCode")) {
        val qrCodeModel =
          if (command.hasKey("QrCodeModel")) getQrCodeModel(command.getString("QrCodeModel")) else getQrCodeModel(
            "No2"
          )
        val qrCodeLevel =
          if (command.hasKey("QrCodeLevel")) getQrCodeLevel(command.getString("QrCodeLevel")) else getQrCodeLevel(
            "H"
          )
        val cell = if (command.hasKey("cell")) command.getInt("cell") else 4
        if (command.hasKey("absolutePosition")) {
          val position = command.getInt("absolutePosition")
          builder.appendQrCodeWithAbsolutePosition(
            command.getString("appendQrCode")!!.toByteArray(encoding!!),
            qrCodeModel,
            qrCodeLevel,
            cell,
            position
          )
        } else if (command.hasKey("alignment")) {
          val alignmentPosition = getAlignment(command.getString("alignment"))
          builder.appendQrCodeWithAlignment(
            command.getString("appendQrCode")!!.toByteArray(encoding!!),
            qrCodeModel,
            qrCodeLevel,
            cell,
            alignmentPosition
          )
        } else builder.appendQrCode(
          command.getString("appendQrCode")!!.toByteArray(encoding!!),
          qrCodeModel,
          qrCodeLevel,
          cell
        )
      } else if (command.hasKey("appendBitmap")) {
        val contentResolver = context!!.contentResolver
        val uriString = command.getString("appendBitmap")
        val diffusion = !command.hasKey("diffusion") || command.getBoolean("diffusion")
        val width = if (command.hasKey("width")) command.getInt("width") else 576
        val bothScale = !command.hasKey("bothScale") || command.getBoolean("bothScale")
        val rotation =
          if (command.hasKey("rotation")) getConverterRotation(command.getString("rotation")) else getConverterRotation(
            "Normal"
          )
        try {
          val imageUri = Uri.parse(uriString)
          val bitmap = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
          } else {
            val imageSource = ImageDecoder.createSource(contentResolver, imageUri)
            ImageDecoder.decodeBitmap(imageSource).copy(Bitmap.Config.RGBA_F16, true)
          }
          if (command.hasKey("absolutePosition")) {
            val position = command.getInt("absolutePosition")
            builder.appendBitmapWithAbsolutePosition(
              bitmap,
              diffusion,
              width,
              bothScale,
              rotation,
              position
            )
          } else if (command.hasKey("alignment")) {
            val alignmentPosition = getAlignment(command.getString("alignment"))
            builder.appendBitmapWithAlignment(
              bitmap,
              diffusion,
              width,
              bothScale,
              rotation,
              alignmentPosition
            )
          } else builder.appendBitmap(bitmap, diffusion, width, bothScale, rotation)
        } catch (e: IOException) {
          // Do nothing
        }
      }
    }
  }

  //ICommandBuilder Constant Functions
  private fun getInternational(international: String?): ICommandBuilder.InternationalType {
    return when (international) {
      "UK" -> ICommandBuilder.InternationalType.UK
      "USA" -> ICommandBuilder.InternationalType.USA
      "France" -> ICommandBuilder.InternationalType.France
      "Germany" -> ICommandBuilder.InternationalType.Germany
      "Denmark" -> ICommandBuilder.InternationalType.Denmark
      "Sweden" -> ICommandBuilder.InternationalType.Sweden
      "Italy" -> ICommandBuilder.InternationalType.Italy
      "Spain" -> ICommandBuilder.InternationalType.Spain
      "Japan" -> ICommandBuilder.InternationalType.Japan
      "Norway" -> ICommandBuilder.InternationalType.Norway
      "Denmark2" -> ICommandBuilder.InternationalType.Denmark2
      "Spain2" -> ICommandBuilder.InternationalType.Spain2
      "LatinAmerica" -> ICommandBuilder.InternationalType.LatinAmerica
      "Korea" -> ICommandBuilder.InternationalType.Korea
      "Ireland" -> ICommandBuilder.InternationalType.Ireland
      "Legal" -> ICommandBuilder.InternationalType.Legal
      else -> ICommandBuilder.InternationalType.USA
    }
  }

  private fun getAlignment(alignment: String?): AlignmentPosition {
    return when (alignment) {
      "Left" -> AlignmentPosition.Left
      "Center" -> AlignmentPosition.Center
      "Right" -> AlignmentPosition.Right
      else -> AlignmentPosition.Left
    }
  }

  private fun getBarcodeSymbology(barcodeSymbology: String?): BarcodeSymbology {
    return when (barcodeSymbology) {
      "Code128" -> BarcodeSymbology.Code128
      "Code39" -> BarcodeSymbology.Code39
      "Code93" -> BarcodeSymbology.Code93
      "ITF" -> BarcodeSymbology.ITF
      "JAN8" -> BarcodeSymbology.JAN8
      "JAN13" -> BarcodeSymbology.JAN13
      "NW7" -> BarcodeSymbology.NW7
      "UPCA" -> BarcodeSymbology.UPCA
      "UPCE" -> BarcodeSymbology.UPCE
      else -> BarcodeSymbology.Code128
    }
  }

  private fun getBarcodeWidth(barcodeWidth: String?): BarcodeWidth {
    if (barcodeWidth == "Mode1") return BarcodeWidth.Mode1
    if (barcodeWidth == "Mode2") return BarcodeWidth.Mode2
    if (barcodeWidth == "Mode3") return BarcodeWidth.Mode3
    if (barcodeWidth == "Mode4") return BarcodeWidth.Mode4
    if (barcodeWidth == "Mode5") return BarcodeWidth.Mode5
    if (barcodeWidth == "Mode6") return BarcodeWidth.Mode6
    if (barcodeWidth == "Mode7") return BarcodeWidth.Mode7
    if (barcodeWidth == "Mode8") return BarcodeWidth.Mode8
    return if (barcodeWidth == "Mode9") BarcodeWidth.Mode9 else BarcodeWidth.Mode2
  }

  private fun getFontStyle(fontStyle: String?): FontStyleType {
    if (fontStyle == "A") return FontStyleType.A
    return if (fontStyle == "B") FontStyleType.B else FontStyleType.A
  }

  private fun getLogoSize(logoSize: String?): LogoSize {
    return when (logoSize) {
      "Normal" -> LogoSize.Normal
      "DoubleWidth" -> LogoSize.DoubleWidth
      "DoubleHeight" -> LogoSize.DoubleHeight
      "DoubleWidthDoubleHeight" -> LogoSize.DoubleWidthDoubleHeight
      else -> LogoSize.Normal
    }
  }

  private fun getCutPaperAction(cutPaperAction: String?): CutPaperAction {
    return when (cutPaperAction) {
      "FullCut" -> CutPaperAction.FullCut
      "FullCutWithFeed" -> CutPaperAction.FullCutWithFeed
      "PartialCut" -> CutPaperAction.PartialCut
      "PartialCutWithFeed" -> CutPaperAction.PartialCutWithFeed
      else -> CutPaperAction.PartialCutWithFeed
    }
  }

  private fun getPeripheralChannel(peripheralChannel: Int): PeripheralChannel {
    return if (peripheralChannel == 1) PeripheralChannel.No1 else if (peripheralChannel == 2) PeripheralChannel.No2 else PeripheralChannel.No1
  }

  private fun getQrCodeModel(qrCodeModel: String?): QrCodeModel {
    return when (qrCodeModel) {
      "No1" -> QrCodeModel.No1
      "No2" -> QrCodeModel.No2
      else -> QrCodeModel.No1
    }
  }

  private fun getQrCodeLevel(qrCodeLevel: String?): QrCodeLevel {
    return when (qrCodeLevel) {
      "H" -> QrCodeLevel.H
      "L" -> QrCodeLevel.L
      "M" -> QrCodeLevel.M
      "Q" -> QrCodeLevel.Q
      else -> QrCodeLevel.H
    }
  }

  private fun getConverterRotation(converterRotation: String?): BitmapConverterRotation {
    return when (converterRotation) {
      "Normal" -> BitmapConverterRotation.Normal
      "Left90" -> BitmapConverterRotation.Left90
      "Right90" -> BitmapConverterRotation.Right90
      "Rotate180" -> BitmapConverterRotation.Rotate180
      else -> BitmapConverterRotation.Normal
    }
  }

  private fun getBlackMarkType(blackMarkType: String?): BlackMarkType {
    return when (blackMarkType) {
      "Valid" -> BlackMarkType.Valid
      "Invalid" -> BlackMarkType.Invalid
      "ValidWithDetection" -> BlackMarkType.ValidWithDetection
      else -> BlackMarkType.Valid
    }
  }

  //Helper functions
  private fun getCodePageType(codePageType: String?): ICommandBuilder.CodePageType {
    return when (codePageType) {
      "CP437" -> ICommandBuilder.CodePageType.CP437
      "CP737" -> ICommandBuilder.CodePageType.CP737
      "CP772" -> ICommandBuilder.CodePageType.CP772
      "CP774" -> ICommandBuilder.CodePageType.CP774
      "CP851" -> ICommandBuilder.CodePageType.CP851
      "CP852" -> ICommandBuilder.CodePageType.CP852
      "CP855" -> ICommandBuilder.CodePageType.CP855
      "CP857" -> ICommandBuilder.CodePageType.CP857
      "CP858" -> ICommandBuilder.CodePageType.CP858
      "CP860" -> ICommandBuilder.CodePageType.CP860
      "CP861" -> ICommandBuilder.CodePageType.CP861
      "CP862" -> ICommandBuilder.CodePageType.CP862
      "CP863" -> ICommandBuilder.CodePageType.CP863
      "CP864" -> ICommandBuilder.CodePageType.CP864
      "CP865" -> ICommandBuilder.CodePageType.CP866
      "CP869" -> ICommandBuilder.CodePageType.CP869
      "CP874" -> ICommandBuilder.CodePageType.CP874
      "CP928" -> ICommandBuilder.CodePageType.CP928
      "CP932" -> ICommandBuilder.CodePageType.CP932
      "CP999" -> ICommandBuilder.CodePageType.CP999
      "CP1001" -> ICommandBuilder.CodePageType.CP1001
      "CP1250" -> ICommandBuilder.CodePageType.CP1250
      "CP1251" -> ICommandBuilder.CodePageType.CP1251
      "CP1252" -> ICommandBuilder.CodePageType.CP1252
      "CP2001" -> ICommandBuilder.CodePageType.CP2001
      "CP3001" -> ICommandBuilder.CodePageType.CP3001
      "CP3002" -> ICommandBuilder.CodePageType.CP3002
      "CP3011" -> ICommandBuilder.CodePageType.CP3011
      "CP3012" -> ICommandBuilder.CodePageType.CP3012
      "CP3021" -> ICommandBuilder.CodePageType.CP3021
      "CP3041" -> ICommandBuilder.CodePageType.CP3041
      "CP3840" -> ICommandBuilder.CodePageType.CP3840
      "CP3841" -> ICommandBuilder.CodePageType.CP3841
      "CP3843" -> ICommandBuilder.CodePageType.CP3843
      "CP3845" -> ICommandBuilder.CodePageType.CP3845
      "CP3846" -> ICommandBuilder.CodePageType.CP3846
      "CP3847" -> ICommandBuilder.CodePageType.CP3847
      "CP3848" -> ICommandBuilder.CodePageType.CP3848
      "UTF8" -> ICommandBuilder.CodePageType.UTF8
      "Blank" -> ICommandBuilder.CodePageType.Blank
      else -> ICommandBuilder.CodePageType.CP998
    }
  }

  private fun getEncoding(encoding: String?): Charset {
    return when (encoding) {
      "US-ASCII" -> Charset.forName("US-ASCII") //English
      "Windows-1252" -> {
        return try {
          Charset.forName("Windows-1252") //French, German, Portuguese, Spanish
        } catch (e: UnsupportedCharsetException) { //not supported using UTF-8 Instead
          Charset.forName("UTF-8")
        }
      }

      "Shift-JIS" -> {
        return try {
          Charset.forName("Shift-JIS")
        } catch (e: UnsupportedCharsetException) {
          Charset.forName("UTF-8")
        }
      }

      "Windows-1251" -> {
        return try {
          Charset.forName("Windows-1251")
        } catch (e: UnsupportedCharsetException) {
          Charset.forName("UTF-8")
        }
      }

      "GB2312" -> {
        return try {
          Charset.forName("GB2312")
        } catch (e: UnsupportedCharsetException) {
          Charset.forName("UTF-8")
        }
      }

      "Big5" -> {
        return try {
          Charset.forName("Big5")
        } catch (e: UnsupportedCharsetException) {
          Charset.forName("UTF-8")
        }
      }

      "UTF-8" -> Charset.forName("UTF-8")
      else -> Charset.forName("US-ASCII")
    }
  }

  private fun sendEvent(dataType: String, info: String?) {
    if (listeners > 0) {
      val reactContext: ReactContext = reactApplicationContext
      val eventName = "starPrntData"
      val params: WritableMap = WritableNativeMap()

      params.putString("dataType", dataType)
      info?.let {
        params.putString("data", info)
      }
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(eventName, params)
    }
  }
}
