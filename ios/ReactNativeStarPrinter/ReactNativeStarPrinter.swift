//
//  ReactNativeStarPrinter.swift
//  ReactNativeStarPrinter
//
//  Created by Sébastien Vray on 02/04/2024.
//

import StarIO10

enum PrintAction: String {
  case cut = "cut"
  case partialCut = "partial-cut"
  case fullDirect = "full-direct"
  case partialDirect = "partial-direct"
}

enum PrintDataType: String {
  case text = "text"
  case image = "image"
  case barcode = "barcode"
}

enum Align: String {
  case center = "center"
  case left = "left"
  case right = "right"
}

struct Style {
  var align: Align?
  var barWidth: Int?
  var bold: Bool?
  var diffusion: Bool?
  var threshold: Int?
  var width: Int?
  var height: Int?
  var heightExpansion: Int?
  var widthExpansion: Int?
  var underlined: Bool?
}

struct PrintCommand {
  var action: PrintAction?
  var data: String?
  var style: Style?
  var type: PrintDataType?
}

class PrinterDiscoverer: StarDeviceDiscoveryManagerDelegate {
  private var manager: StarDeviceDiscoveryManager? = nil
  private var lastPromise: (RCTPromiseResolveBlock?, RCTPromiseRejectBlock?) = (nil, nil)
  
  func interfaceTypeToString(interface: InterfaceType) -> String {
    return switch interface {
      case .lan:
        "lan"
      case .bluetooth:
        "bluetooth"
      case .bluetoothLE:
        "BLE"
      case .usb:
        "usb"
      default:
        "unkown"
    }
  }
  
  func discover(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    do {
      lastPromise = (resolve as RCTPromiseResolveBlock, reject as RCTPromiseRejectBlock)
      manager?.stopDiscovery()
      try manager = StarDeviceDiscoveryManagerFactory.create(interfaceTypes: [InterfaceType.bluetooth, InterfaceType.bluetoothLE, InterfaceType.lan, InterfaceType.usb])
      manager?.discoveryTime = 10000
      manager?.delegate = self
      try manager?.startDiscovery()
    } catch StarIO10Error.illegalDeviceState(message: let message, errorCode: let errorCode) {
      if errorCode == StarIO10ErrorCode.bluetoothUnavailable {
        // Example of error: Bluetooth capability of iOS device is disabled.
        // This may be due to the host device's Bluetooth being off.
        reject("PRINTER_SEARCH_ERROR_NO_BLUETOOH", message, nil)
      } else {
        reject("PRINTER_SEARCH_ERROR", "Error while searching for printer", nil)
      }
    } catch let error {
      reject("PRINTER_SEARCH_ERROR", "Error while searching for printer", error)
    }
  }
  
  func manager(_ manager: StarDeviceDiscoveryManager, didFind printer: StarPrinter) {
    lastPromise.0?([
      "connection-settings": [
        "identifier": printer.connectionSettings.identifier,
        "interface": self.interfaceTypeToString(interface: printer.connectionSettings.interfaceType)
      ],
      "information": [
        "model": String(describing: printer.information?.model ?? StarPrinterModel.unknown),
        "emulation":  String(describing: printer.information?.emulation ?? StarPrinterEmulation.unknown)
      ]
    ])
  }
  
  func managerDidFinishDiscovery(_ manager: StarDeviceDiscoveryManager) {
    Swift.print("Printer discovery finished.")
  }
}

@objc(ReactNativeStarPrinter)
class ReactNativeStarPrinter: RCTEventEmitter, PrinterDelegate, DrawerDelegate, InputDeviceDelegate, DisplayDelegate {
  var RNStarPrnt_hasListeners = false
  let printerDiscoverer = PrinterDiscoverer()
  var starPrinter: StarPrinter? = nil
  
  override init() {
    super.init()
  }
  
  deinit {
    if (starPrinter != nil) {
      Task { [starPrinter] in
        await starPrinter!.close()
      }
    }
  }
  
  @objc
  override static func requiresMainQueueSetup() -> Bool {
    return true
  }
  
  @objc
  override var methodQueue: DispatchQueue {
    get {
      return DispatchQueue(label: "fr.wavy.x.react-native-star-printer")
    }
  }
  
  override func supportedEvents() -> [String]!
  {
    return [
      "displayConnected",
      "displayCommunicationError",
      "displayDisconnected",
      "inputDeviceCommunicationError",
      "inputDeviceConnected",
      "inputDeviceDisconnected",
      "inputDeviceReadData",
      "printerCommunicationError",
      "printerDrawerCommunicationError",
      "printerDrawerClosed",
      "printerDrawerOpened",
      "printerIsReady",
      "printerHasError",
      "printerPaperIsReady",
      "printerPaperIsNearEmpty",
      "printerPaperIsEmpty",
      "printerCoverOpened",
      "printerCoverClosed"
    ]
  }
  
  func printer(_ printer: StarPrinter, communicationErrorDidOccur error: Error) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerCommunicationError", body: error.localizedDescription)
    }
  }
  
  func printerIsReady(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerIsReady", body: nil)
    }
  }
  
  func printerDidHaveError(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerHasError", body: nil)
    }
  }
  
  func printerIsPaperReady(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerPaperIsReady", body: nil)
    }
  }
  
  func printerIsPaperNearEmpty(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerPaperIsNearEmpty", body: nil)
    }
  }
  
  func printerIsPaperEmpty(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerPaperIsEmpty", body: nil)
    }
  }
  
  func printerIsCoverOpen(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerCoverOpened", body: nil)
    }
  }
  
  func printerIsCoverClose(_ printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerCoverClosed", body: nil)
    }
  }
  
  func drawer(printer: StarPrinter, communicationErrorDidOccur error: Error) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "printerDrawerCommunicationError", body: error.localizedDescription)
    }
  }
  
  func drawer(printer: StarPrinter, didSwitch openCloseSignal: Bool) {
    if (RNStarPrnt_hasListeners) {
      if (openCloseSignal) {
        sendEvent(withName: "printerDrawerOpened", body: nil)
      } else {
        sendEvent(withName: "printerDrawerClosed", body: nil)
      }
    }
  }
  
  func inputDevice(printer: StarPrinter, communicationErrorDidOccur error: Error) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "inputDeviceCommunicationError", body: error.localizedDescription)
    }
  }
  
  func inputDeviceDidConnect(printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "inputDeviceConnected", body: nil)
    }
  }
  
  func inputDeviceDidDisconnect(printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "inputDeviceDisconnected", body: nil)
    }
  }
  
  func inputDevice(printer: StarPrinter, didReceive data: Data) {
    Swift.print("Input device red data")
    if (RNStarPrnt_hasListeners) {
      Swift.print("Sending read data event")
      sendEvent(withName: "inputDeviceReadData", body: String(decoding: data, as: UTF8.self))
    }
  }
  
  func display(printer: StarPrinter, communicationErrorDidOccur error: Error) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "displayCommunicationError", body: error.localizedDescription)
    }
  }
  
  func displayDidConnect(printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "displayConnected", body: nil)
    }
  }
  
  func displayDidDisconnect(printer: StarPrinter) {
    if (RNStarPrnt_hasListeners) {
      sendEvent(withName: "displayDisconnected", body: nil)
    }
  }
  
  // Will be called when this module's first listener is added.
  @objc
  override func startObserving() {
    self.RNStarPrnt_hasListeners = true
    // Set up any upstream listeners or background tasks as necessary
  }
  
  // Will be called when this module's last listener is removed, or on dealloc.
  @objc
  func StopObserving() {
    self.RNStarPrnt_hasListeners = false
    // Remove upstream listeners, stop unnecessary background tasks
  }
  
  @objc
  func searchPrinter(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    printerDiscoverer.discover(resolve, rejecter: reject);
  }
  
  func stringToInterfaceType(interface: String) -> InterfaceType {
    return switch interface {
      case "lan":
        InterfaceType.lan
      case "bluetooth":
        InterfaceType.bluetooth
      case "BLE":
        InterfaceType.bluetoothLE
      case "usb":
        InterfaceType.usb
      default:
        InterfaceType.unknown
    }
  }
  
  @objc
  func connect(
    _ identifier: String,
    interface: String,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      let connectionSettings = StarConnectionSettings(interfaceType: self.stringToInterfaceType(interface: interface), identifier: identifier, autoSwitchInterface: true)
      if (starPrinter != nil) {
        await Task {
          await starPrinter!.close()
        }.value
      }
      Task {
        do {
          starPrinter = StarPrinter(connectionSettings)
          starPrinter!.displayDelegate = self
          starPrinter!.drawerDelegate = self
          starPrinter!.inputDeviceDelegate = self
          starPrinter!.printerDelegate = self
          try await starPrinter?.open()
          resolve(true)
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.inUse(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_PRINTER_IN_USE", message, nil)
        } catch StarIO10Error.notFound(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_PRINTER_NOT_FOUND", message, nil)
        } catch StarIO10Error.argument(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        } catch StarIO10Error.illegalDeviceState(message: let message, errorCode: let errorCode) {
          switch errorCode {
            case StarIO10ErrorCode.networkUnavailable:
              reject("PRINTER_OPEN_NETWORK_UNAVAILABLE", message, nil)
            case StarIO10ErrorCode.bluetoothUnavailable:
              reject("PRINTER_OPEN_BLUETOOTH_UNAVAILABLE", message, nil)
            default:
              reject("PRINTER_OPEN_ILLEGAL_DEVICE_STATE", message, nil)
          }
        } catch StarIO10Error.unsupportedModel(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_UNSUPPORTED_MODEL", message, nil)
        }
      }
    }
  }
  
  @objc
  func getStatus(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      Task {
        do {
          let status = try await notNilStarPrinter.getStatus()
          
          resolve([
            "hasError": status.hasError,
            "coverOpen": status.coverOpen,
            "drawerOpenCloseSignal": status.drawerOpenCloseSignal,
            "paperEmpty": status.paperEmpty,
            "paperNearEmpty": status.paperNearEmpty,
            "cutterError": status.detail.cutterError as Any?,
            "paperSeparatorError": status.detail.paperSeparatorError,
            "paperJamError": status.detail.paperJamError,
            "rollPositionError": status.detail.rollPositionError,
            "paperPresent": status.detail.paperPresent,
            "drawerOpenError": status.detail.drawerOpenError,
            "printUnitOpen": status.detail.printUnitOpen,
            "detectedPaperWidth": status.detail.detectedPaperWidth,
          ])
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("PRINTER_GET_STATUS_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("PRINTER_GET_STATUS_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        }
      }
    } else {
      reject("PRINTER_GET_STATUS_NO_PRINTER_CONNECTION", "Not connected to any printer", nil)
    }
  }
  
  @objc
  func openCashDrawer(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      Task {
        do {
          let builder = StarXpandCommand.StarXpandCommandBuilder()
          _ = builder.addDocument(StarXpandCommand.DocumentBuilder.init()
            .addDrawer(StarXpandCommand.DrawerBuilder()
              .actionOpen(StarXpandCommand.Drawer.OpenParameter()
                .setChannel(.no1)
              )
            )
          )
          try await notNilStarPrinter.print(command: builder.getCommands())
          resolve(true)
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_DRAWER_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_DRAWER_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_DRAWER_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        } catch StarIO10Error.unknown(message: let message, errorCode: _) {
          reject("PRINTER_OPEN_DRAWER_UNKNOWN_ERROR", message, nil)
        }
      }
    } else {
      reject("PRINTER_OPEN_DRAWER_NO_PRINTER_CONNECTION", "Not connected to any printer", nil)
    }
  }
  
  func setAlignment(command: PrintCommand, printerBuilder: StarXpandCommand.PrinterBuilder) -> Void {
    switch (command.style?.align) {
      case .left:
        _ = printerBuilder.styleAlignment(.left)
      case .center:
        _ = printerBuilder.styleAlignment(.center)
      case .right:
        _ = printerBuilder.styleAlignment(.right)
      default:
        break
    }
  }
  
  func getPrinterInternationalCharacterType(name: String) -> StarXpandCommand.Printer.InternationalCharacterType {
    switch name {
      case "usa":
        return .usa
      case "france":
        return .france
      case "germany":
        return .germany
      case "uk":
        return .uk
      case "denmark":
        return .denmark
      case "sweden":
        return .sweden
      case "italy":
        return .italy
      case "spain":
        return .spain
      case "japan":
        return .japan
      case "norway":
        return .norway
      case "denmark2":
        return .denmark2
      case "spain2":
        return .spain2
      case "latinAmerica":
        return .latinAmerica
      case "korea":
        return .korea
      case "ireland":
        return .ireland
      case "slovenia":
        return .slovenia
      case "croatia":
        return .croatia
      case "china":
        return .china
      case "vietnam":
        return .vietnam
      case "arabic":
        return .arabic
      case "legal":
        return .legal
      default:
        return .usa
    }
  }
  
  @objc
  func print(
    _ commands: [[String:Any]],
    charset: String = "usa",
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      let printCommands: [PrintCommand] = commands.map { command in
        var printCommand = PrintCommand()
        
        if let data = command["data"] as? String {
          printCommand.data = data
        }
        if (command["type"] != nil) {
          printCommand.type = PrintDataType(rawValue: command["type"] as! String)
        }
        if (command["action"] != nil) {
          printCommand.action = PrintAction(rawValue: command["action"] as! String)
        }
        if let style = command["style"] as? NSDictionary {
          printCommand.style = Style()
          if let align = style["align"] as? String {
            printCommand.style?.align = Align(rawValue: align)
          }
          if let barWidth = style["barWidth"] as? Int {
            printCommand.style?.barWidth = barWidth
          }
          if let bold = style["bold"] as? Bool {
            printCommand.style?.bold = bold
          }
          if let heightExpansion = style["heightExpansion"] as? Int {
            printCommand.style?.heightExpansion = heightExpansion
          }
          if let underlined = style["underlined"] as? Bool {
            printCommand.style?.underlined = underlined
          }
          if let widthExpansion = style["widthExpansion"] as? Int {
            printCommand.style?.widthExpansion = widthExpansion
          }
          if let width = style["width"] as? Int {
            printCommand.style?.width = width
          }
          if let height = style["height"] as? Int {
            printCommand.style?.height = height
          }
          if let threshold = style["threshold"] as? Int {
            printCommand.style?.threshold = threshold
          }
          if let diffusion = style["diffusion"] as? Bool {
            printCommand.style?.diffusion = diffusion
          }
        }
        return printCommand
      }
      
      Task {
        do {
          let commandBuilder = StarXpandCommand.StarXpandCommandBuilder()
          let printerBuilder = StarXpandCommand.PrinterBuilder()
          let documentBuilder = StarXpandCommand.DocumentBuilder()
          
          _ = printerBuilder.styleInternationalCharacter(self.getPrinterInternationalCharacterType(name: charset))
          
          for command in printCommands {
            self.setAlignment(command: command, printerBuilder: printerBuilder)
            _ = printerBuilder
              .styleBold(command.style?.bold ?? false)
              .styleMagnification(
                StarXpandCommand.MagnificationParameter(
                  width: (command.style?.widthExpansion ?? 0) + 1,
                  height: (command.style?.heightExpansion ?? 0) + 1
                )
              )
            
            if command.type == PrintDataType.text, let data = command.data {
              _ = printerBuilder.actionPrintText(data)
            }
            if (command.action == PrintAction.cut) {
              _ = printerBuilder.actionCut(.full)
            }
            if (command.action == PrintAction.partialCut) {
              _ = printerBuilder.actionCut(.partial)
            }
            if (command.action == PrintAction.fullDirect) {
              _ = printerBuilder.actionCut(.fullDirect)
            }
            if (command.action == PrintAction.partialDirect) {
              _ = printerBuilder.actionCut(.partialDirect)
            }
            if command.type == PrintDataType.image, let url = command.data {
              if let image = await self.downloadImage(url), let width = command.style?.width {
                let imageParameter = StarXpandCommand.Printer.ImageParameter(image: image, width: width)
                if let diffusion = command.style?.diffusion {
                  _ = imageParameter.setEffectDiffusion(diffusion)
                }
                if let threshold = command.style?.threshold {
                  _ = imageParameter.setThreshold(threshold)
                }
                _ = printerBuilder.actionPrintImage(imageParameter)
              }
            }
            if command.type == PrintDataType.barcode, let barcodeData = command.data {
              let barcodeParameter = StarXpandCommand.Printer.BarcodeParameter(content: barcodeData, symbology: .code128).setHeight(Double(command.style?.height ?? 5)).setPrintHRI(true)
              if let barWidth = command.style?.barWidth {
                _ = barcodeParameter.setBarDots(barWidth)
              }
              _ = printerBuilder.actionPrintBarcode(barcodeParameter)
            }
          }
          _ = commandBuilder.addDocument(documentBuilder.addPrinter(printerBuilder))
          try await notNilStarPrinter.print(command: commandBuilder.getCommands())
          resolve(true)
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        } catch StarIO10Error.unknown(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_UNKNOWN_ERROR", message, nil)
        }
      }
    } else {
      reject("PRINTER_PRINT_NO_PRINTER_CONNECTION", "Not connected to any printer", nil)
    }
  }
  
  func downloadImage(_ from: String) async -> UIImage? {
    if let url = URL(string: from) {
      do {
        let (data, _) = try await URLSession.shared.data(from: url)
        return UIImage(data: data)
      } catch {
        Swift.print(error)
      }
    }
    return nil
  }
  
  func getDisplayInternationalCharacterType(name: String) -> StarXpandCommand.Display.InternationalCharacterType {
    switch name {
      case "usa":
        return .usa
      case "france":
        return .france
      case "germany":
        return .germany
      case "uk":
        return .uk
      case "denmark":
        return .denmark
      case "sweden":
        return .sweden
      case "italy":
        return .italy
      case "spain":
        return .spain
      case "japan":
        return .japan
      case "norway":
        return .norway
      case "denmark2":
        return .denmark2
      case "spain2":
        return .spain2
      case "latinAmerica":
        return .latinAmerica
      case "korea":
        return .korea
      default:
        return .usa
    }
  }
  
  func getDisplayCursorState(name: String) -> StarXpandCommand.Display.CursorState {
    switch name {
      case "on":
        return .on
      case "off":
        return .off
      case "blink":
        return .blink
      default:
        return .off
    }
  }
  
  @objc
  func showTextOnDisplay(
    _ content: String,
    backLight: Bool = true,
    contrast: Int = 0,
    cursorState: String = "off",
    charset: String = "usa",
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      Task {
        do {
          let commandBuilder = StarXpandCommand.StarXpandCommandBuilder()
          let displayBuilder = StarXpandCommand.DisplayBuilder()
          let documentBuilder = StarXpandCommand.DocumentBuilder()
          
          _ = displayBuilder.styleInternationalCharacter(self.getDisplayInternationalCharacterType(name: charset))
            .actionSetCursorState(self.getDisplayCursorState(name: cursorState))
            .actionSetBackLightState(backLight)
            .actionSetContrast(StarXpandCommand.Display.Contrast(rawValue: contrast) ?? .default)
            .actionShowText(content)
          _ = commandBuilder.addDocument(documentBuilder.addDisplay(displayBuilder))
          try await notNilStarPrinter.print(command: commandBuilder.getCommands())
          resolve(true)
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("DISPLAY_SHOW_TEXT_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("DISPLAY_SHOW_TEXT_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("DISPLAY_SHOW_TEXT_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        } catch StarIO10Error.unknown(message: let message, errorCode: _) {
          reject("DISPLAY_SHOW_TEXT_UNKNOWN_ERROR", message, nil)
        }
      }
    } else {
      reject("DISPLAY_SHOW_TEXT_NO_PRINTER_CONNECTION", "Not connected to any printer", nil)
    }
  }
  
  @objc
  func clearDisplay(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      Task {
        do {
          let commandBuilder = StarXpandCommand.StarXpandCommandBuilder()
          
          _ = commandBuilder.addDocument(
            StarXpandCommand.DocumentBuilder().addDisplay(
              StarXpandCommand.DisplayBuilder().actionClearAll()
            )
          )
          try await notNilStarPrinter.print(command: commandBuilder.getCommands())
          resolve(true)
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("DISPLAY_CLEAR_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("DISPLAY_CLEAR_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("DISPLAY_CLEAR_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        } catch StarIO10Error.unknown(message: let message, errorCode: _) {
          reject("DISPLAY_CLEAR_UNKNOWN_ERROR", message, nil)
        }
      }
    } else {
      reject("DISPLAY_CLEAR_NO_PRINTER_CONNECTION", "Not connected to any printer", nil)
    }
  }
  
  @objc
  func disconnect(
    _ resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      Task {
        await notNilStarPrinter.close()
        resolve(true)
        starPrinter = nil
      }
    }
  }
}
