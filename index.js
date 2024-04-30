
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-star-printer' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n';

const ReactNativeStarPrinter = NativeModules.ReactNativeStarPrinter
  ? NativeModules.ReactNativeStarPrinter
  : new Proxy(
    {},
    {
      get() {
        throw new Error(LINKING_ERROR);
      },
    }
  );

export const PrinterOpenErrorCodes = {
  PRINTER_OPEN_INVALID_OPERATION: 'PRINTER_OPEN_INVALID_OPERATION',
  PRINTER_OPEN_COMMUNICATION_ERROR: 'PRINTER_OPEN_COMMUNICATION_ERROR',
  PRINTER_OPEN_PRINTER_IN_USE: 'PRINTER_OPEN_PRINTER_IN_USE',
  PRINTER_OPEN_PRINTER_NOT_FOUND: 'PRINTER_OPEN_PRINTER_NOT_FOUND',
  PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID: 'PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID',
  PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER: 'PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER',
  PRINTER_OPEN_BLUETOOTH_UNAVAILABLE: 'PRINTER_OPEN_BLUETOOTH_UNAVAILABLE',
  PRINTER_OPEN_ILLEGAL_DEVICE_STATE: 'PRINTER_OPEN_ILLEGAL_DEVICE_STATE',
  PRINTER_OPEN_UNSUPPORTED_MODEL: 'PRINTER_OPEN_UNSUPPORTED_MODEL',
};

export const PrinterGetStatusErrorCodes = {
  PRINTER_GET_STATUS_INVALID_OPERATION: 'PRINTER_GET_STATUS_INVALID_OPERATION', // Not connected to printer
  PRINTER_GET_STATUS_COMMUNICATION_ERROR: 'PRINTER_GET_STATUS_COMMUNICATION_ERROR',
  PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER: 'PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER',
  PRINTER_GET_STATUS_NO_PRINTER_CONNECTION: 'PRINTER_GET_STATUS_NO_PRINTER_CONNECTION',
};

export const PrintDataType = {
  text: "text",
  image: "image",
  barcode: "barcode",
};

export const Align = {
  center: "center",
  left: "left",
  right: "right",
};

export const PrintAction = {
  cut: "cut",
  partialCut: "partial-cut",
  fullDirect: "full-direct",
  partialDirect: "partial-direct",
};

export const InternationalCharacterType ={
  usa: "usa",
  france: "france",
  germany: "germany",
  uk: "uk",
  denmark: "denmark",
  sweden: "sweden",
  italy: "italy",
  spain: "spain",
  japan: "japan",
  norway: "norway",
  denmark2: "denmark2",
  spain2: "spain2",
  latinAmerica: "latinAmerica",
  korea: "korea",
  ireland: "ireland",
  slovenia: "slovenia",
  croatia: "croatia",
  china: "china",
  vietnam: "vietnam",
  arabic: "arabic",
  legal: "legal",
};

export const DisplayInternationalCharacterType = {
  usa: "usa",
  france: "france",
  germany: "germany",
  uk: "uk",
  denmark: "denmark",
  sweden: "sweden",
  italy: "italy",
  spain: "spain",
  japan: "japan",
  norway: "norway",
  denmark2: "denmark2",
  spain2: "spain2",
  latinAmerica: "latinAmerica",
  korea: "korea",
};

export const DisplayCursorState = {
  ON: "on",
  OFF: "off",
  BLINK: "blink",
};

export class StarPRNT {

  static StarPRNTManagerEmitter = new NativeEventEmitter(ReactNativeStarPrinter);

  /**
  * Constant for Emulation
  */
  static Emulation = {
    StarPRNT: 'StarPRNT',
    StarPRNTL: 'StarPRNTL',
    StarLine: 'StarLine',
    StarGraphic: 'StarGraphic',
    EscPos: 'EscPos',
    EscPosMobile: 'EscPosMobile',
    StarDotImpact: 'StarDotImpact',
  };

  /**
   * Constant for possible Encoding
   */
  static Encoding = {
    USASCII: 'US-ASCII',
    Windows1252: 'Windows-1252',
    ShiftJIS: 'Shift-JIS',
    Windows1251: 'Windows-1251',
    GB2312: 'GB2312',
    Big5: 'Big5',
    UTF8: 'UTF-8'
  };

  /**
   * CodePageType constants
   */
  static CodePageType = {
    CP737: 'CP737',
    CP772: 'CP772',
    CP774: 'CP774',
    CP851: 'CP851',
    CP852: 'CP852',
    CP855: 'CP855',
    CP857: 'CP857',
    CP858: 'CP858',
    CP860: 'CP860',
    CP861: 'CP861',
    CP862: 'CP862',
    CP863: 'CP863',
    CP864: 'CP864',
    CP865: 'CP865',
    CP869: 'CP869',
    CP874: 'CP874',
    CP928: 'CP928',
    CP932: 'CP932',
    CP999: 'CP999',
    CP1001: 'CP1001',
    CP1250: 'CP1250',
    CP1251: 'CP1251',
    CP1252: 'CP1252',
    CP2001: 'CP2001',
    CP3001: 'CP3001',
    CP3002: 'CP3002',
    CP3011: 'CP3011',
    CP3012: 'CP3012',
    CP3021: 'CP3021',
    CP3041: 'CP3041',
    CP3840: 'CP3840',
    CP3841: 'CP3841',
    CP3843: 'CP3843',
    CP3845: 'CP3845',
    CP3846: 'CP3846',
    CP3847: 'CP3847',
    CP3848: 'CP3848',
    UTF8: 'UTF8',
    Blank: 'Blank'
  };

  /**
   * Constant for possible InternationalType
   */
  static InternationalType = {
    UK: 'UK',
    USA: 'USA',
    France: 'France',
    Germany: 'Germany',
    Denmark: 'Denmark',
    Sweden: 'Sweden',
    Italy: 'Italy',
    Spain: 'Spain',
    Japan: 'Japan',
    Norway: 'Norway',
    Denmark2: 'Denmark2',
    Spain2: 'Spain2',
    LatinAmerica: 'LatinAmerica',
    Korea: 'Korea',
    Ireland: 'Ireland',
    Legal: 'Legal'
  };

  /**
   * Constant for possible FontStyleType
   */
  static FontStyleType = {
    /** Font-A (12 x 24 dots) / Specify 7 x 9 font (half dots) */
    A: 'A',
    /** Font-B (9 x 24 dots) / Specify 5 x 9 font (2P-1) */
    B: 'B'
  };

  /**
   * Constant for possible CutPaperAction
   */
  static CutPaperAction = {
    FullCut: 'FullCut',
    FullCutWithFeed: 'FullCutWithFeed',
    PartialCut: 'PartialCut',
    PartialCutWithFeed: 'PartialCutWithFeed'
  };

  /**
   * Constant for possible BlackMarkType
   */
  static BlackMarkType = {
    Valid: 'Valid',
    Invalid: 'Invalid',
    ValidWithDetection: 'ValidWithDetection'
  };

  /**
   * Constant for possible AlignmentPosition
   */
  static AlignmentPosition = {
    Left: 'Left',
    Center: 'Center',
    Right: 'Right'
  };

  /**
   * Constant for possible LogoSize
   */
  static LogoSize = {
    Normal: 'Normal',
    DoubleWidth: 'DoubleWidth',
    DoubleHeight: 'DoubleHeight',
    DoubleWidthDoubleHeight: 'DoubleWidthDoubleHeight'
  };

  /**
   * Constant for possible BarcodeSymbology
   */
  static BarcodeSymbology = {
    Code128: 'Code128',
    Code39: 'Code39',
    Code93: 'Code93',
    ITF: 'ITF',
    JAN8: 'JAN8',
    JAN13: 'JAN13',
    NW7: 'NW7',
    UPCA: 'UPCA',
    UPCE: 'UPCE'
  };

  /**
   * Constant for possible BarcodeWidth
   */
  static BarcodeWidth = {
    Mode1: 'Mode1',
    Mode2: 'Mode2',
    Mode3: 'Mode3',
    Mode4: 'Mode4',
    Mode5: 'Mode5',
    Mode6: 'Mode6',
    Mode7: 'Mode7',
    Mode8: 'Mode8',
    Mode9: 'Mode9'
  };

  /**
   * Constant for possible QrCodeModel
   */
  static QrCodeModel = {
    No1: 'No1',
    No2: 'No2'
  };

  /**
   * Constant for possible QrCodeLevel
   */
  static QrCodeLevel = {
    H: 'H',
    L: 'L',
    M: 'M',
    Q: 'Q'
  };

  /**
   * Constant for possible BitmapConverterRotation
   */
  static BitmapConverterRotation = {
    Normal: 'Normal',
    Left90: 'Left90',
    Right90: 'Right90',
    Rotate180: 'Rotate180'
  };

  /**
   * Find printers available
   * @param {string} type Iterface Type: All, LAN, Bluetooth, USB
   * @return {Promise<Printers>} Returns a promise that resolves with an array of printers
   */
  static portDiscovery(type) {
    return ReactNativeStarPrinter.portDiscovery(type);
  }

  /**
   * Checks the status of the printer
   * @param {string} port printer name i.e BT:StarMicronics
   * @param {string} emulation StarPrinter Emulation type: "StarPRNT", "StarPRNTL", "StarLine", "StarGraphic", "EscPos", "EscPosMobile", "StarDotImpact"
   * @return {Promise<PrinterStatus>} Returns a promise that resolves with the printer status object
   */
  static checkStatus(port, emulation) {
    return ReactNativeStarPrinter.checkStatus(port, emulation);
  }
  /**
   * Allows you to connect to the printer, keep the connection alive and receive status updates through an observable
   * @param {string} port printer name i.e BT:StarMicronics.
   * @param {string} emulation StarPrinter Emulation type: "StarPRNT", "StarPRNTL", "StarLine", "StarGraphic", "EscPos", "EscPosMobile", "StarDotImpact"
   * @param {boolean} hasBarcodeReader If device has an attached barcode reader i.e mPOP
   * @return {Observable<any>} Success! if connected or error message string returned by the SDK.
   */
  static connect(port, emulation, hasBarcodeReader) {
    hasBarcodeReader = (hasBarcodeReader) ? true : false;
    return ReactNativeStarPrinter.connect(port, emulation, hasBarcodeReader);
  }

  /**
   * iOS uses the new SDK StarXPAND
   * @param {string} port -> identifier Device identifier obtained with the `searchPrinter` method
   * @param {InterfaceType} emulation -> interfaceType Interface to use to connect to the device
   */
  static connectiOS(identifier, interfaceType) {
    return ReactNativeStarPrinter.connect(identifier, interfaceType);
  }

  /**
   * Allows to disconnect (close the connection to the peripherals), this is useful to avoid keeping alive a connection when not in the app to save device battery
   * (energy consumption). You should call this function when the app is paused or closed.
   * @return {Promise<any>} Success! if connected or error message string returned by the SDK.
   */
  static disconnect() {
    return ReactNativeStarPrinter.disconnect();
  }

  /**
 * Sends an Array of commands to the command buffer using the Android ICommandBuilderInterface or iOS ISCBBuilderInterface
 * @param {string} emulation  StarPrinter Emulation type: "StarPRNT", "StarPRNTL", "StarLine", "StarGraphic", "EscPos", "EscPosMobile", "StarDotImpact"
 * @param {CommandsArray} commandsArray  each command in the array should be an instance of the PrintCommand object. Example [{append:"text"}, {"openCashDrawer: 1"}]
 * * @param {string} port Optional. printer name i.e BT:StarMicronics. If not set, a printer connected via StarIOExtManager using the connect() function will be used.
 * @return {Promise<any>} Success! if printed correctly or error message string returned by the SDK.
 */
  static print(emulation, commandsArray, port) {
    return ReactNativeStarPrinter.print(port, emulation, commandsArray);
  }

  static showPriceIndicator(emulation, commandsArray, port) {
    return ReactNativeStarPrinter.showPriceIndicator(port, emulation, commandsArray);
  }

  static cleanCustomerDisplay(emulation, commandsArray, port) {
    return ReactNativeStarPrinter.cleanCustomerDisplay(port, emulation, commandsArray);
  }

  static turnCustomerDisplay(turnTo, emulation, commandsArray, port) {
    return ReactNativeStarPrinter.turnCustomerDisplay(turnTo, port, emulation, commandsArray);
  }

  /**
 * Same as print, but don't perform a checkStatus before and after print
 * @param {string} emulation  StarPrinter Emulation type: "StarPRNT", "StarPRNTL", "StarLine", "StarGraphic", "EscPos", "EscPosMobile", "StarDotImpact"
 * @param {CommandsArray} commandsArray  each command in the array should be an instance of the PrintCommand object. Example [{append:"text"}, {"openCashDrawer: 1"}]
 * * @param {string} port Optional. printer name i.e BT:StarMicronics. If not set, a printer connected via StarIOExtManager using the connect() function will be used.
 * @return {Promise<any>} Success! if printed correctly or error message string returned by the SDK.
 */
  static optimisticPrint(emulation, commandsArray, port) {
    return ReactNativeStarPrinter.optimisticPrint(port, emulation, commandsArray);
  }

  static setAutoConnect(autoConnectEnabled) {
    return ReactNativeStarPrinter.setAutoConnect(autoConnectEnabled);
  }

  // iOS only uses the new SDK StarXPAND
  static searchPrinter() {
    return ReactNativeStarPrinter.searchPrinter();
  }

  // iOS only uses the new SDK StarXPAND
  /**
   * Get the status of the printer currently the device is connected to
   */
  static getStatus() {
    return ReactNativeStarPrinter.getStatus();
  }

  // iOS only uses the new SDK StarXPAND
  /**
   * Opens the printer cash drawer
   */
  static openCashDrawer() {
    return ReactNativeStarPrinter.openCashDrawer();
  }

  // iOS only uses the new SDK StarXPAND
  static printiOS(commands, internationalCharacterType) {
    return ReactNativeStarPrinter.print(commands, internationalCharacterType);
  }
  // iOS only uses the new SDK StarXPAND
  static displayText(content, backlight = true, contrast = 0, cursorState = 'off', internationalCharacterType = 'usa')  {
    return ReactNativeStarPrinter.showTextOnDisplay(content, backlight, contrast, cursorState, internationalCharacterType);
  }

  // iOS only
  static clearDisplay() {
    return ReactNativeStarPrinter.clearDisplay();
  }
}
