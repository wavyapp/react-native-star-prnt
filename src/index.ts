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

export class StarPrinter {
  private static _instance?: StarPrinter;
  private _nativeEventEmitter = new NativeEventEmitter(ReactNativeStarPrinter);

  static getInstance(): StarPrinter {
    if (!StarPrinter._instance) {
      StarPrinter._instance = new StarPrinter();
    }
    return StarPrinter._instance;
  }

  addListener(event: PrinterEvent, cb: (data?: string) => unknown) {
    return this._nativeEventEmitter.addListener(event, cb);
  }

  searchPrinter(): Promise<FoundPrinter> {
    return ReactNativeStarPrinter.searchPrinter();
  }

  /**
  * @param {string} identifier Device identifier obtained with the `searchPrinter` method
  * @param {InterfaceType} interfaceType Interface to use to connect to the device
  */
  connect(identifier: string, interfaceType: InterfaceType): Promise<boolean> {
    return ReactNativeStarPrinter.connect(identifier, interfaceType);
  }

  /**
   * Allows to disconnect (close the connection to the peripherals), this is useful to avoid keeping alive a connection when not in the app to save device battery
   * (energy consumption). You should call this function when the app is paused or closed.
   * @return {Promise<any>} Success! if connected or error message string returned by the SDK.
   */
  disconnect(): Promise<Boolean> {
    return ReactNativeStarPrinter.disconnect();
  }

  print(commands: PrintCommand[], internationalCharacterType?: PrintInternationalCharacterType): Promise<boolean> {
    return ReactNativeStarPrinter.print(commands, internationalCharacterType);
  }

  getStatus(): Promise<Status> {
    return ReactNativeStarPrinter.getStatus();
  }

  openCashDrawer(): Promise<boolean> {
    return ReactNativeStarPrinter.openCashDrawer();
  }

  displayText(content: String, backlight?: boolean, contrast?: number, cursorState?: DisplayCursorState, internationalCharacterType?: DisplayInternationalCharacterType): Promise<boolean> {
    console.log(backlight || true, contrast || .0, cursorState, internationalCharacterType);
    return ReactNativeStarPrinter.showTextOnDisplay(content, backlight || true, contrast || .0, cursorState, internationalCharacterType);
  }

  clearDisplay(): Promise<boolean> {
    return ReactNativeStarPrinter.clearDisplay();
  }
}

export enum PrinterEvent {
  DISPLAY_CONNECTED = 'displayConnected',
  DISPLAY_COMMUNICATION_ERROR = 'displayCommunicationError',
  DISPLAY_DISCONNECTED = 'displayDisconnected',
  INPUT_DEVICE_COMMUNICATION_ERROR = 'inputDeviceCommunicationError',
  INPUT_DEVICE_CONNECTED = 'inputDeviceConnected',
  INPUT_DEVICE_DISCONNECTED = 'inputDeviceDisconnected',
  INPUT_DEVICE_READ_DATA = 'inputDeviceReadData',
  PRINTER_COMMUNICATION_ERROR = 'printerCommunicationError',
  PRINTER_DRAWER_COMMUNICATION_ERROR = 'printerDrawerCommunicationError',
  PRINTER_DRAWER_CLOSED = 'printerDrawerClosed',
  PRINTER_DRAWER_OPENED = 'printerDrawerOpened',
  PRINTER_IS_READY = 'printerIsReady',
  PRINTER_HAS_ERROR = 'printerHasError',
  PRINTER_PAPER_IS_READY = 'printerPaperIsReady',
  PRINTER_PAPER_IS_NEAR_EMPTY = 'printerPaperIsNearEmpty',
  PRINTER_PAPER_IS_EMPTY = 'printerPaperIsEmpty',
  PRINTER_COVER_OPENED = 'printerCoverOpened',
  PRINTER_COVER_CLOSED = 'printerCoverClosed',
}

export enum InterfaceType {
  BLUETOOTH = 'bluetooth',
  BLE = 'BLE',
  LAN = 'lan',
  USB = 'usb',
  unknown = 'unknown',
}

export type FoundPrinter = {
  "connection-settings": {
    identifier: string;
    interface: InterfaceType
  };
  information: {
    emulation: string;
    model: string;
  };
};

export enum PrinterOpenErrorCodes {
  PRINTER_OPEN_INVALID_OPERATION = 'PRINTER_OPEN_INVALID_OPERATION',
  PRINTER_OPEN_COMMUNICATION_ERROR = 'PRINTER_OPEN_COMMUNICATION_ERROR',
  PRINTER_OPEN_PRINTER_IN_USE = 'PRINTER_OPEN_PRINTER_IN_USE',
  PRINTER_OPEN_PRINTER_NOT_FOUND = 'PRINTER_OPEN_PRINTER_NOT_FOUND',
  PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID = 'PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID',
  PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER = 'PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER',
  PRINTER_OPEN_BLUETOOTH_UNAVAILABLE = 'PRINTER_OPEN_BLUETOOTH_UNAVAILABLE',
  PRINTER_OPEN_USB_UNAVAILABLE = 'PRINTER_OPEN_USB_UNAVAILABLE',
  PRINTER_OPEN_ILLEGAL_DEVICE_STATE = 'PRINTER_OPEN_ILLEGAL_DEVICE_STATE',
  PRINTER_OPEN_UNSUPPORTED_MODEL = 'PRINTER_OPEN_UNSUPPORTED_MODEL',
}

export type Status = {
  coverOpen: boolean;
  cutterError: boolean;
  detectedPaperWidth: number | null;
  drawerOpenCloseSignal: boolean;
  drawerOpenError: boolean;
  hasError: boolean;
  paperEmpty: boolean;
  paperJamError: boolean;
  paperNearEmpty: boolean;
  paperPresent: boolean;
  paperSeparatorError: boolean;
  printUnitOpen: boolean;
  rollPositionError: boolean;
};

export enum PrinterGetStatusErrorCodes {
  PRINTER_GET_STATUS_INVALID_OPERATION = 'PRINTER_GET_STATUS_INVALID_OPERATION', // Not connected to printer
  PRINTER_GET_STATUS_COMMUNICATION_ERROR = 'PRINTER_GET_STATUS_COMMUNICATION_ERROR',
  PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER = 'PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER',
  PRINTER_GET_STATUS_NO_PRINTER_CONNECTION = 'PRINTER_GET_STATUS_NO_PRINTER_CONNECTION',
}

export enum PrintDataType {
  text = "text",
  image = "image",
  barcode = "barcode",
}

export enum Align {
  center = "center",
  left = "left",
  right = "right",
}

export type Style = {
  align?: Align;
  barWidth?: number; // Width of barcode bars in dots
  bold?: boolean;
  diffusion?: boolean; // Use diffusion effect
  heightExpansion?: number;
  height?: number;
  threshold?: number; // Image binarization threshold
  underlined?: boolean;
  width?: number; // Image width in dots
  widthExpansion?: number;
};

export type PrintCommand = {
  data?: string;
  style?: Style;
  type?: PrintDataType;
} | {
  action?: PrintAction.cut;
} | {
  action?: PrintAction.fullDirect;
} | {
  action?: PrintAction.partialCut;
} | {
  action?: PrintAction.partialDirect;
} | {
  action?: PrintAction.feedLine;
} | {
  action?: PrintAction.paperFeed;
  actionArguments?: { height: number };
} | {
  action?: PrintAction.printLineSeperator;
  actionArguments?: {
    width: number; // Length of horizontal line (mm)
    thickness?: number; // Thickness of horizontal line (mm)
    style?: RuleLineStyle;
    xOffset?: number; // Start position of horizontal line (mm)
  };
};

export enum PrintAction {
  printLineSeperator = "print-line-separator",
  feedLine = "feed-line",
  paperFeed = "paper-feed",
  cut = "cut",
  partialCut = "partial-cut",
  fullDirect = "full-direct", // Full cut without paper feed
  partialDirect = "partial-direct", // Partial cut without paper feed
}

export enum RuleLineStyle {
  SINGLE = "single",
  DOUBLE = "double",
}

export enum PrintInternationalCharacterType {
  usa = "usa",
  france = "france",
  germany = "germany",
  uk = "uk",
  denmark = "denmark",
  sweden = "sweden",
  italy = "italy",
  spain = "spain",
  japan = "japan",
  norway = "norway",
  denmark2 = "denmark2",
  spain2 = "spain2",
  latinAmerica = "latinAmerica",
  korea = "korea",
  ireland = "ireland",
  slovenia = "slovenia",
  croatia = "croatia",
  china = "china",
  vietnam = "vietnam",
  arabic = "arabic",
  legal = "legal",
}

export enum DisplayInternationalCharacterType {
  usa = "usa",
  france = "france",
  germany = "germany",
  uk = "uk",
  denmark = "denmark",
  sweden = "sweden",
  italy = "italy",
  spain = "spain",
  japan = "japan",
  norway = "norway",
  denmark2 = "denmark2",
  spain2 = "spain2",
  latinAmerica = "latinAmerica",
  korea = "korea",
}

export enum DisplayCursorState {
  ON = "on",
  OFF = "off",
  BLINK = "blink",
}