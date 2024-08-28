"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.StarPrinter = exports.RuleLineStyle = exports.PrinterSearchErrorCodes = exports.PrinterPrintErrorCodes = exports.PrinterOpenErrorCodes = exports.PrinterGetStatusErrorCodes = exports.PrinterEvent = exports.PrintInternationalCharacterType = exports.PrintDataType = exports.PrintAction = exports.InterfaceType = exports.DisplayInternationalCharacterType = exports.DisplayCursorState = exports.Align = void 0;
var _reactNative = require("react-native");
const LINKING_ERROR = `The package 'react-native-star-printer' doesn't seem to be linked. Make sure: \n\n` + _reactNative.Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n';
const ReactNativeStarPrinter = _reactNative.NativeModules.ReactNativeStarPrinter ? _reactNative.NativeModules.ReactNativeStarPrinter : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
class StarPrinter {
  _nativeEventEmitter = new _reactNative.NativeEventEmitter(ReactNativeStarPrinter);
  static getInstance() {
    if (!StarPrinter._instance) {
      StarPrinter._instance = new StarPrinter();
    }
    return StarPrinter._instance;
  }
  addListener(event, cb) {
    return this._nativeEventEmitter.addListener(event, cb);
  }
  searchPrinter() {
    return ReactNativeStarPrinter.searchPrinter();
  }

  /**
  * @param {string} identifier Device identifier obtained with the `searchPrinter` method
  * @param {InterfaceType} interfaceType Interface to use to connect to the device
  */
  connect(identifier, interfaceType) {
    return ReactNativeStarPrinter.connect(identifier, interfaceType);
  }

  /**
   * Allows to disconnect (close the connection to the peripherals), this is useful to avoid keeping alive a connection when not in the app to save device battery
   * (energy consumption). You should call this function when the app is paused or closed.
   * @return {Promise<any>} Success! if connected or error message string returned by the SDK.
   */
  disconnect() {
    return ReactNativeStarPrinter.disconnect();
  }
  print(commands, internationalCharacterType) {
    return ReactNativeStarPrinter.print(commands, internationalCharacterType);
  }
  getStatus() {
    return ReactNativeStarPrinter.getStatus();
  }
  openCashDrawer() {
    return ReactNativeStarPrinter.openCashDrawer();
  }
  displayText(content, backlight, contrast, cursorState, internationalCharacterType) {
    console.log(backlight || true, contrast || .0, cursorState, internationalCharacterType);
    return ReactNativeStarPrinter.showTextOnDisplay(content, backlight || true, contrast || .0, cursorState, internationalCharacterType);
  }
  clearDisplay() {
    return ReactNativeStarPrinter.clearDisplay();
  }
}
exports.StarPrinter = StarPrinter;
let PrinterEvent = exports.PrinterEvent = /*#__PURE__*/function (PrinterEvent) {
  PrinterEvent["DISPLAY_CONNECTED"] = "displayConnected";
  PrinterEvent["DISPLAY_COMMUNICATION_ERROR"] = "displayCommunicationError";
  PrinterEvent["DISPLAY_DISCONNECTED"] = "displayDisconnected";
  PrinterEvent["INPUT_DEVICE_COMMUNICATION_ERROR"] = "inputDeviceCommunicationError";
  PrinterEvent["INPUT_DEVICE_CONNECTED"] = "inputDeviceConnected";
  PrinterEvent["INPUT_DEVICE_DISCONNECTED"] = "inputDeviceDisconnected";
  PrinterEvent["INPUT_DEVICE_READ_DATA"] = "inputDeviceReadData";
  PrinterEvent["PRINTER_COMMUNICATION_ERROR"] = "printerCommunicationError";
  PrinterEvent["PRINTER_DRAWER_COMMUNICATION_ERROR"] = "printerDrawerCommunicationError";
  PrinterEvent["PRINTER_DRAWER_CLOSED"] = "printerDrawerClosed";
  PrinterEvent["PRINTER_DRAWER_OPENED"] = "printerDrawerOpened";
  PrinterEvent["PRINTER_IS_READY"] = "printerIsReady";
  PrinterEvent["PRINTER_HAS_ERROR"] = "printerHasError";
  PrinterEvent["PRINTER_PAPER_IS_READY"] = "printerPaperIsReady";
  PrinterEvent["PRINTER_PAPER_IS_NEAR_EMPTY"] = "printerPaperIsNearEmpty";
  PrinterEvent["PRINTER_PAPER_IS_EMPTY"] = "printerPaperIsEmpty";
  PrinterEvent["PRINTER_COVER_OPENED"] = "printerCoverOpened";
  PrinterEvent["PRINTER_COVER_CLOSED"] = "printerCoverClosed";
  return PrinterEvent;
}({});
let InterfaceType = exports.InterfaceType = /*#__PURE__*/function (InterfaceType) {
  InterfaceType["BLUETOOTH"] = "bluetooth";
  InterfaceType["BLE"] = "BLE";
  InterfaceType["LAN"] = "lan";
  InterfaceType["USB"] = "usb";
  InterfaceType["unknown"] = "unknown";
  return InterfaceType;
}({});
let PrinterOpenErrorCodes = exports.PrinterOpenErrorCodes = /*#__PURE__*/function (PrinterOpenErrorCodes) {
  PrinterOpenErrorCodes["PRINTER_OPEN_INVALID_OPERATION"] = "PRINTER_OPEN_INVALID_OPERATION";
  PrinterOpenErrorCodes["PRINTER_OPEN_COMMUNICATION_ERROR"] = "PRINTER_OPEN_COMMUNICATION_ERROR";
  PrinterOpenErrorCodes["PRINTER_OPEN_PRINTER_IN_USE"] = "PRINTER_OPEN_PRINTER_IN_USE";
  PrinterOpenErrorCodes["PRINTER_OPEN_PRINTER_NOT_FOUND"] = "PRINTER_OPEN_PRINTER_NOT_FOUND";
  PrinterOpenErrorCodes["PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID"] = "PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID";
  PrinterOpenErrorCodes["PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER"] = "PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER";
  PrinterOpenErrorCodes["PRINTER_OPEN_BLUETOOTH_UNAVAILABLE"] = "PRINTER_OPEN_BLUETOOTH_UNAVAILABLE";
  PrinterOpenErrorCodes["PRINTER_OPEN_USB_UNAVAILABLE"] = "PRINTER_OPEN_USB_UNAVAILABLE";
  PrinterOpenErrorCodes["PRINTER_OPEN_ILLEGAL_DEVICE_STATE"] = "PRINTER_OPEN_ILLEGAL_DEVICE_STATE";
  PrinterOpenErrorCodes["PRINTER_OPEN_UNSUPPORTED_MODEL"] = "PRINTER_OPEN_UNSUPPORTED_MODEL";
  return PrinterOpenErrorCodes;
}({});
let PrinterGetStatusErrorCodes = exports.PrinterGetStatusErrorCodes = /*#__PURE__*/function (PrinterGetStatusErrorCodes) {
  PrinterGetStatusErrorCodes["PRINTER_GET_STATUS_INVALID_OPERATION"] = "PRINTER_GET_STATUS_INVALID_OPERATION";
  PrinterGetStatusErrorCodes["PRINTER_GET_STATUS_COMMUNICATION_ERROR"] = "PRINTER_GET_STATUS_COMMUNICATION_ERROR";
  PrinterGetStatusErrorCodes["PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER"] = "PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER";
  PrinterGetStatusErrorCodes["PRINTER_GET_STATUS_NO_PRINTER_CONNECTION"] = "PRINTER_GET_STATUS_NO_PRINTER_CONNECTION";
  return PrinterGetStatusErrorCodes;
}({});
let PrintDataType = exports.PrintDataType = /*#__PURE__*/function (PrintDataType) {
  PrintDataType["text"] = "text";
  PrintDataType["image"] = "image";
  PrintDataType["barcode"] = "barcode";
  return PrintDataType;
}({});
let Align = exports.Align = /*#__PURE__*/function (Align) {
  Align["center"] = "center";
  Align["left"] = "left";
  Align["right"] = "right";
  return Align;
}({});
let PrintAction = exports.PrintAction = /*#__PURE__*/function (PrintAction) {
  PrintAction["printLineSeperator"] = "print-line-separator";
  PrintAction["feedLine"] = "feed-line";
  PrintAction["paperFeed"] = "paper-feed";
  PrintAction["cut"] = "cut";
  PrintAction["partialCut"] = "partial-cut";
  PrintAction["fullDirect"] = "full-direct";
  PrintAction["partialDirect"] = "partial-direct";
  return PrintAction;
}({}); // Partial cut without paper feed
let RuleLineStyle = exports.RuleLineStyle = /*#__PURE__*/function (RuleLineStyle) {
  RuleLineStyle["SINGLE"] = "single";
  RuleLineStyle["DOUBLE"] = "double";
  return RuleLineStyle;
}({});
let PrintInternationalCharacterType = exports.PrintInternationalCharacterType = /*#__PURE__*/function (PrintInternationalCharacterType) {
  PrintInternationalCharacterType["usa"] = "usa";
  PrintInternationalCharacterType["france"] = "france";
  PrintInternationalCharacterType["germany"] = "germany";
  PrintInternationalCharacterType["uk"] = "uk";
  PrintInternationalCharacterType["denmark"] = "denmark";
  PrintInternationalCharacterType["sweden"] = "sweden";
  PrintInternationalCharacterType["italy"] = "italy";
  PrintInternationalCharacterType["spain"] = "spain";
  PrintInternationalCharacterType["japan"] = "japan";
  PrintInternationalCharacterType["norway"] = "norway";
  PrintInternationalCharacterType["denmark2"] = "denmark2";
  PrintInternationalCharacterType["spain2"] = "spain2";
  PrintInternationalCharacterType["latinAmerica"] = "latinAmerica";
  PrintInternationalCharacterType["korea"] = "korea";
  PrintInternationalCharacterType["ireland"] = "ireland";
  PrintInternationalCharacterType["slovenia"] = "slovenia";
  PrintInternationalCharacterType["croatia"] = "croatia";
  PrintInternationalCharacterType["china"] = "china";
  PrintInternationalCharacterType["vietnam"] = "vietnam";
  PrintInternationalCharacterType["arabic"] = "arabic";
  PrintInternationalCharacterType["legal"] = "legal";
  return PrintInternationalCharacterType;
}({});
let DisplayInternationalCharacterType = exports.DisplayInternationalCharacterType = /*#__PURE__*/function (DisplayInternationalCharacterType) {
  DisplayInternationalCharacterType["usa"] = "usa";
  DisplayInternationalCharacterType["france"] = "france";
  DisplayInternationalCharacterType["germany"] = "germany";
  DisplayInternationalCharacterType["uk"] = "uk";
  DisplayInternationalCharacterType["denmark"] = "denmark";
  DisplayInternationalCharacterType["sweden"] = "sweden";
  DisplayInternationalCharacterType["italy"] = "italy";
  DisplayInternationalCharacterType["spain"] = "spain";
  DisplayInternationalCharacterType["japan"] = "japan";
  DisplayInternationalCharacterType["norway"] = "norway";
  DisplayInternationalCharacterType["denmark2"] = "denmark2";
  DisplayInternationalCharacterType["spain2"] = "spain2";
  DisplayInternationalCharacterType["latinAmerica"] = "latinAmerica";
  DisplayInternationalCharacterType["korea"] = "korea";
  return DisplayInternationalCharacterType;
}({});
let DisplayCursorState = exports.DisplayCursorState = /*#__PURE__*/function (DisplayCursorState) {
  DisplayCursorState["ON"] = "on";
  DisplayCursorState["OFF"] = "off";
  DisplayCursorState["BLINK"] = "blink";
  return DisplayCursorState;
}({});
let PrinterSearchErrorCodes = exports.PrinterSearchErrorCodes = /*#__PURE__*/function (PrinterSearchErrorCodes) {
  PrinterSearchErrorCodes["PRINTER_SEARCH_ERROR_NO_BLUETOOTH"] = "PRINTER_SEARCH_ERROR_NO_BLUETOOTH";
  PrinterSearchErrorCodes["PRINTER_SEARCH_ERROR"] = "PRINTER_SEARCH_ERROR";
  PrinterSearchErrorCodes["PRINTER_SEARCH_ERROR_BLUETOOTH_PERMISSION_DENIED"] = "PRINTER_SEARCH_ERROR_BLUETOOTH_PERMISSION_DENIED";
  return PrinterSearchErrorCodes;
}({}); // Android only
let PrinterPrintErrorCodes = exports.PrinterPrintErrorCodes = /*#__PURE__*/function (PrinterPrintErrorCodes) {
  PrinterPrintErrorCodes["PRINTER_PRINT_DEVICE_ERROR"] = "PRINTER_PRINT_DEVICE_ERROR";
  PrinterPrintErrorCodes["PRINTER_PRINT_PRINTER_HOLDING_PAPER"] = "PRINTER_PRINT_PRINTER_HOLDING_PAPER";
  PrinterPrintErrorCodes["PRINTER_PRINT_PRINTING_TIMEOUT"] = "PRINTER_PRINT_PRINTING_TIMEOUT";
  PrinterPrintErrorCodes["PRINTER_PRINT_INVALID_DEVICE_STATUS"] = "PRINTER_PRINT_INVALID_DEVICE_STATUS";
  PrinterPrintErrorCodes["PRINTER_PRINT_INVALID_OPERATION"] = "PRINTER_PRINT_INVALID_OPERATION";
  PrinterPrintErrorCodes["PRINTER_PRINT_COMMUNICATION_ERROR"] = "PRINTER_PRINT_COMMUNICATION_ERROR";
  PrinterPrintErrorCodes["PRINTER_PRINT_INVALID_RESPONSE_FROM_PRINTER"] = "PRINTER_PRINT_INVALID_RESPONSE_FROM_PRINTER";
  PrinterPrintErrorCodes["PRINTER_PRINT_UNKNOWN_ERROR"] = "PRINTER_PRINT_UNKNOWN_ERROR";
  PrinterPrintErrorCodes["PRINTER_PRINT_NO_PRINTER_CONNECTION"] = "PRINTER_PRINT_NO_PRINTER_CONNECTION";
  return PrinterPrintErrorCodes;
}({});
//# sourceMappingURL=index.js.map