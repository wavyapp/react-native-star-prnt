import StarIO10

enum ReactNativeStarPrinterError: Error {
  case invalidArgument(message: String?)
}

class PrinterDiscoverer: StarDeviceDiscoveryManagerDelegate {
  private var manager: StarDeviceDiscoveryManager? = nil
  private var discoverPromises: [(RCTPromiseResolveBlock, RCTPromiseRejectBlock, Bool)] = []
  private var foundPrinter: StarPrinter? = nil

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
      manager?.stopDiscovery()
      try manager = StarDeviceDiscoveryManagerFactory.create(interfaceTypes: [InterfaceType.bluetooth, InterfaceType.bluetoothLE, InterfaceType.lan, InterfaceType.usb])
      manager?.discoveryTime = 10000
      manager?.delegate = self
      try manager?.startDiscovery()
      discoverPromises.append((resolve as RCTPromiseResolveBlock, reject as RCTPromiseRejectBlock, false))
    } catch StarIO10Error.illegalDeviceState(message: let message, errorCode: let errorCode) {
      if errorCode == StarIO10ErrorCode.bluetoothUnavailable {
        // Example of error: Bluetooth capability of iOS device is disabled.
        // This may be due to the host device's Bluetooth being off.
        reject("PRINTER_SEARCH_ERROR_NO_BLUETOOTH", message, nil)
      } else {
        reject("PRINTER_SEARCH_ERROR", "Error while searching for printer", nil)
      }
    } catch let error {
      reject("PRINTER_SEARCH_ERROR", "Error while searching for printer", error)
    }
  }

  func manager(_ manager: StarDeviceDiscoveryManager, didFind printer: StarPrinter) {
    for (index, promise) in discoverPromises.enumerated() {
      let (resolve, _, resolved) = promise
      if (!resolved) {
        resolve([
          "connection-settings": [
            "identifier": printer.connectionSettings.identifier,
            "interface": self.interfaceTypeToString(interface: printer.connectionSettings.interfaceType)
          ],
          "information": [
            "model": String(describing: printer.information?.model ?? StarPrinterModel.unknown),
            "emulation":  String(describing: printer.information?.emulation ?? StarPrinterEmulation.unknown)
          ]
        ])
        discoverPromises[index].2 = true
      }
    }
    foundPrinter = printer
  }

  func managerDidFinishDiscovery(_ manager: StarDeviceDiscoveryManager) {
    if (foundPrinter == nil) {
      for (index, promise) in discoverPromises.enumerated() {
        let (_, reject, resolved) = promise
        if (!resolved) {
          reject("PRINTER_SEARCH_NO_PRINTER_AVAILABLE", "StarIO SDK found no printer to connect to", nil)
          discoverPromises[index].2 = true
        }
      }
    }
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
    if (RNStarPrnt_hasListeners) {
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

  func setAlignment(command: Print, printerBuilder: StarXpandCommand.PrinterBuilder) -> Void {
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

  @objc
  func print(
    _ commands: [[String:Any]],
    charset: String = "usa",
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    if let notNilStarPrinter = starPrinter {
      Task {
        do {
          let printCommands: [PrinterCommand] = try commands.map { command in
            return try printerCommandFactory(command)
          }
          let commandBuilder = StarXpandCommand.StarXpandCommandBuilder()
          let printerBuilder = StarXpandCommand.PrinterBuilder()
          let documentBuilder = StarXpandCommand.DocumentBuilder()

          _ = printerBuilder.styleInternationalCharacter(getPrinterInternationalCharacterType(name: charset))

          for command in printCommands {
            if let printCommand = command as? Print {
              self.setAlignment(command: printCommand, printerBuilder: printerBuilder)

              _ = printerBuilder
                .styleBold(printCommand.style?.bold ?? false)
                .styleMagnification(
                  StarXpandCommand.MagnificationParameter(
                    width: (printCommand.style?.widthExpansion ?? 0) + 1,
                    height: (printCommand.style?.heightExpansion ?? 0) + 1
                  )
                )

              if (printCommand.type == PrintDataType.text) {
                _ = printerBuilder.actionPrintText(printCommand.data)
              }

              if (printCommand.type == PrintDataType.image) {
                if let image = await self.downloadImage(printCommand.data), let width = printCommand.style?.width {
                  let imageParameter = StarXpandCommand.Printer.ImageParameter(image: image, width: width)
                  if let diffusion = printCommand.style?.diffusion {
                    _ = imageParameter.setEffectDiffusion(diffusion)
                  }
                  if let threshold = printCommand.style?.threshold {
                    _ = imageParameter.setThreshold(threshold)
                  }
                  _ = printerBuilder.actionPrintImage(imageParameter)
                }
              }
              if (printCommand.type == PrintDataType.barcode) {
                let barcodeParameter = StarXpandCommand.Printer.BarcodeParameter(content: printCommand.data, symbology: .code128).setHeight(Double(printCommand.style?.height ?? 5)).setPrintHRI(true)
                if let barWidth = printCommand.style?.barWidth {
                  _ = barcodeParameter.setBarDots(barWidth)
                }
                _ = printerBuilder.actionPrintBarcode(barcodeParameter)
              }
            }
            if let printAction = command as? PrinterAction {
              if (printAction.action == Action.cut) {
                _ = printerBuilder.actionCut(.full)
              }
              if (printAction.action == Action.partialCut) {
                _ = printerBuilder.actionCut(.partial)
              }
              if (printAction.action == Action.fullDirect) {
                _ = printerBuilder.actionCut(.fullDirect)
              }
              if (printAction.action == Action.partialDirect) {
                _ = printerBuilder.actionCut(.partialDirect)
              }
              if (printAction.action == Action.feedLine) {
                _ = printerBuilder.actionFeedLine(1)
              }
              if (printAction.action == Action.paperFeed) {
                _ = printerBuilder.actionFeed(Double(printAction.args?["height"] as? Double ?? 1.0))
              }
              if (printAction.action == Action.printRuledLine) {
                let ruledLineWidth: Double = (printAction.args?["width"] as? Double) ?? 48.0
                let ruledLineParameters = StarXpandCommand.Printer.RuledLineParameter(width: ruledLineWidth)

                if let thickness = printAction.args?["thickness"] as? Double {
                  _ = ruledLineParameters.setThickness(thickness)
                }
                if let xOffset = printAction.args?["xOffset"] as? Double {
                  _ = ruledLineParameters.setX(xOffset)
                }
                if let lineStyle = printAction.args?["style"] as? String {
                  if (lineStyle.lowercased() == "double") {
                    _ = ruledLineParameters.setLineStyle(StarXpandCommand.Printer.LineStyle.double)
                  }
                  if (lineStyle.lowercased() == "single") {
                    _ = ruledLineParameters.setLineStyle(StarXpandCommand.Printer.LineStyle.single)
                  }
                }
                _ = printerBuilder.styleAlignment(StarXpandCommand.Printer.Alignment.center).actionPrintRuledLine(ruledLineParameters)
              }
            }
          }
          _ = commandBuilder.addDocument(documentBuilder.addPrinter(printerBuilder))
          try await notNilStarPrinter.print(command: commandBuilder.getCommands())
          resolve(true)
        } catch StarIO10Error.unprintable(message: let message, errorCode: let errorCode, status: _) {
          switch errorCode {
            case StarIO10ErrorCode.deviceHasError:
              reject("PRINTER_PRINT_DEVICE_ERROR", message, nil)
            case StarIO10ErrorCode.printerHoldingPaper:
              reject("PRINTER_PRINT_PRINTER_HOLDING_PAPER", message, nil)
            case StarIO10ErrorCode.printingTimeout:
              reject("PRINTER_PRINT_PRINTING_TIMEOUT", message, nil)
            default:
              reject("PRINTER_PRINT_INVALID_DEVICE_STATUS", message, nil)
          }
        } catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_INVALID_OPERATION", message, nil)
        } catch StarIO10Error.communication(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_COMMUNICATION_ERROR", message, nil)
        } catch StarIO10Error.badResponse(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_INVALID_RESPONSE_FROM_PRINTER", message, nil)
        } catch StarIO10Error.unknown(message: let message, errorCode: _) {
          reject("PRINTER_PRINT_UNKNOWN_ERROR", message, nil)
        } catch ReactNativeStarPrinterError.invalidArgument(message: let message) {
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

          _ = displayBuilder.styleInternationalCharacter(getDisplayInternationalCharacterType(name: charset))
            .actionSetCursorState(getDisplayCursorState(name: cursorState))
            .actionSetBackLightState(backLight)
            .actionSetContrast(StarXpandCommand.Display.Contrast(rawValue: contrast) ?? .default)
            .actionShowText(content)
          _ = commandBuilder.addDocument(documentBuilder.addDisplay(displayBuilder))
          try await notNilStarPrinter.print(command: commandBuilder.getCommands())
          resolve(true)
        } catch StarIO10Error.unprintable(message: let message, errorCode: let errorCode, status: _) {
          switch errorCode {
            case StarIO10ErrorCode.deviceHasError:
              reject("DISPLAY_SHOW_TEXT_DEVICE_ERROR", message, nil)
            case StarIO10ErrorCode.printerHoldingPaper:
              reject("DISPLAY_SHOW_TEXT_PRINTER_HOLDING_PAPER", message, nil)
            case StarIO10ErrorCode.printingTimeout:
              reject("DISPLAY_SHOW_TEXT_PRINTING_TIMEOUT", message, nil)
            default:
              reject("DISPLAY_SHOW_TEXT_INVALID_DEVICE_STATUS", message, nil)
          }
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
        } catch StarIO10Error.unprintable(message: let message, errorCode: let errorCode, status: _) {
          switch errorCode {
            case StarIO10ErrorCode.deviceHasError:
              reject("DISPLAY_CLEAR_DEVICE_ERROR", message, nil)
            case StarIO10ErrorCode.printerHoldingPaper:
              reject("DISPLAY_CLEAR_PRINTER_HOLDING_PAPER", message, nil)
            case StarIO10ErrorCode.printingTimeout:
              reject("DISPLAY_CLEAR_PRINTING_TIMEOUT", message, nil)
            default:
              reject("DISPLAY_CLEAR_INVALID_DEVICE_STATUS", message, nil)
          }
        }  catch StarIO10Error.invalidOperation(message: let message, errorCode: _) {
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
    } else {
      resolve(true)
    }
  }
}
