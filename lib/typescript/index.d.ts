export declare class StarPrinter {
    private static _instance?;
    private _nativeEventEmitter;
    static getInstance(): StarPrinter;
    addListener(event: PrinterEvent, cb: (data?: string) => unknown): import("react-native").EmitterSubscription;
    searchPrinter(): Promise<FoundPrinter>;
    /**
    * @param {string} identifier Device identifier obtained with the `searchPrinter` method
    * @param {InterfaceType} interfaceType Interface to use to connect to the device
    */
    connect(identifier: string, interfaceType: InterfaceType): Promise<boolean>;
    /**
     * Allows to disconnect (close the connection to the peripherals), this is useful to avoid keeping alive a connection when not in the app to save device battery
     * (energy consumption). You should call this function when the app is paused or closed.
     * @return {Promise<any>} Success! if connected or error message string returned by the SDK.
     */
    disconnect(): Promise<Boolean>;
    print(commands: PrintCommand[], internationalCharacterType?: PrintInternationalCharacterType): Promise<boolean>;
    getStatus(): Promise<Status>;
    openCashDrawer(): Promise<boolean>;
    displayText(content: String, backlight?: boolean, contrast?: number, cursorState?: DisplayCursorState, internationalCharacterType?: DisplayInternationalCharacterType): Promise<boolean>;
    clearDisplay(): Promise<boolean>;
}
export declare enum PrinterEvent {
    DISPLAY_CONNECTED = "displayConnected",
    DISPLAY_COMMUNICATION_ERROR = "displayCommunicationError",
    DISPLAY_DISCONNECTED = "displayDisconnected",
    INPUT_DEVICE_COMMUNICATION_ERROR = "inputDeviceCommunicationError",
    INPUT_DEVICE_CONNECTED = "inputDeviceConnected",
    INPUT_DEVICE_DISCONNECTED = "inputDeviceDisconnected",
    INPUT_DEVICE_READ_DATA = "inputDeviceReadData",
    PRINTER_COMMUNICATION_ERROR = "printerCommunicationError",
    PRINTER_DRAWER_COMMUNICATION_ERROR = "printerDrawerCommunicationError",
    PRINTER_DRAWER_CLOSED = "printerDrawerClosed",
    PRINTER_DRAWER_OPENED = "printerDrawerOpened",
    PRINTER_IS_READY = "printerIsReady",
    PRINTER_HAS_ERROR = "printerHasError",
    PRINTER_PAPER_IS_READY = "printerPaperIsReady",
    PRINTER_PAPER_IS_NEAR_EMPTY = "printerPaperIsNearEmpty",
    PRINTER_PAPER_IS_EMPTY = "printerPaperIsEmpty",
    PRINTER_COVER_OPENED = "printerCoverOpened",
    PRINTER_COVER_CLOSED = "printerCoverClosed"
}
export declare enum InterfaceType {
    BLUETOOTH = "bluetooth",
    BLE = "BLE",
    LAN = "lan",
    USB = "usb",
    unknown = "unknown"
}
export type FoundPrinter = {
    "connection-settings": {
        identifier: string;
        interface: InterfaceType;
    };
    information: {
        emulation: string;
        model: string;
    };
};
export declare enum PrinterOpenErrorCodes {
    PRINTER_OPEN_INVALID_OPERATION = "PRINTER_OPEN_INVALID_OPERATION",
    PRINTER_OPEN_COMMUNICATION_ERROR = "PRINTER_OPEN_COMMUNICATION_ERROR",
    PRINTER_OPEN_PRINTER_IN_USE = "PRINTER_OPEN_PRINTER_IN_USE",
    PRINTER_OPEN_PRINTER_NOT_FOUND = "PRINTER_OPEN_PRINTER_NOT_FOUND",
    PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID = "PRINTER_OPEN_IDENTIFIER_FORMAT_INVALID",
    PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER = "PRINTER_OPEN_INVALID_RESPONSE_FROM_PRINTER",
    PRINTER_OPEN_BLUETOOTH_UNAVAILABLE = "PRINTER_OPEN_BLUETOOTH_UNAVAILABLE",
    PRINTER_OPEN_USB_UNAVAILABLE = "PRINTER_OPEN_USB_UNAVAILABLE",
    PRINTER_OPEN_ILLEGAL_DEVICE_STATE = "PRINTER_OPEN_ILLEGAL_DEVICE_STATE",
    PRINTER_OPEN_UNSUPPORTED_MODEL = "PRINTER_OPEN_UNSUPPORTED_MODEL"
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
export declare enum PrinterGetStatusErrorCodes {
    PRINTER_GET_STATUS_INVALID_OPERATION = "PRINTER_GET_STATUS_INVALID_OPERATION",// Not connected to printer
    PRINTER_GET_STATUS_COMMUNICATION_ERROR = "PRINTER_GET_STATUS_COMMUNICATION_ERROR",
    PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER = "PRINTER_GET_STATUS_INVALID_RESPONSE_FROM_PRINTER",
    PRINTER_GET_STATUS_NO_PRINTER_CONNECTION = "PRINTER_GET_STATUS_NO_PRINTER_CONNECTION"
}
export declare enum PrintDataType {
    text = "text",
    image = "image",
    barcode = "barcode"
}
export declare enum Align {
    center = "center",
    left = "left",
    right = "right"
}
export type Style = {
    align?: Align;
    barWidth?: number;
    bold?: boolean;
    diffusion?: boolean;
    heightExpansion?: number;
    height?: number;
    threshold?: number;
    underlined?: boolean;
    width?: number;
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
    actionArguments?: {
        height: number;
    };
} | {
    action?: PrintAction.printLineSeperator;
    actionArguments?: {
        width: number;
        thickness?: number;
        style?: RuleLineStyle;
        xOffset?: number;
    };
};
export declare enum PrintAction {
    printLineSeperator = "print-line-separator",
    feedLine = "feed-line",
    paperFeed = "paper-feed",
    cut = "cut",
    partialCut = "partial-cut",
    fullDirect = "full-direct",// Full cut without paper feed
    partialDirect = "partial-direct"
}
export declare enum RuleLineStyle {
    SINGLE = "single",
    DOUBLE = "double"
}
export declare enum PrintInternationalCharacterType {
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
    legal = "legal"
}
export declare enum DisplayInternationalCharacterType {
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
    korea = "korea"
}
export declare enum DisplayCursorState {
    ON = "on",
    OFF = "off",
    BLINK = "blink"
}
export declare enum PrinterSearchErrorCodes {
    PRINTER_SEARCH_ERROR_NO_BLUETOOTH = "PRINTER_SEARCH_ERROR_NO_BLUETOOTH",
    PRINTER_SEARCH_ERROR = "PRINTER_SEARCH_ERROR",
    PRINTER_SEARCH_ERROR_BLUETOOTH_PERMISSION_DENIED = "PRINTER_SEARCH_ERROR_BLUETOOTH_PERMISSION_DENIED"
}
export declare enum PrinterPrintErrorCodes {
    PRINTER_PRINT_DEVICE_ERROR = "PRINTER_PRINT_DEVICE_ERROR",
    PRINTER_PRINT_PRINTER_HOLDING_PAPER = "PRINTER_PRINT_PRINTER_HOLDING_PAPER",
    PRINTER_PRINT_PRINTING_TIMEOUT = "PRINTER_PRINT_PRINTING_TIMEOUT",
    PRINTER_PRINT_INVALID_DEVICE_STATUS = "PRINTER_PRINT_INVALID_DEVICE_STATUS",
    PRINTER_PRINT_INVALID_OPERATION = "PRINTER_PRINT_INVALID_OPERATION",
    PRINTER_PRINT_COMMUNICATION_ERROR = "PRINTER_PRINT_COMMUNICATION_ERROR",
    PRINTER_PRINT_INVALID_RESPONSE_FROM_PRINTER = "PRINTER_PRINT_INVALID_RESPONSE_FROM_PRINTER",
    PRINTER_PRINT_UNKNOWN_ERROR = "PRINTER_PRINT_UNKNOWN_ERROR",
    PRINTER_PRINT_NO_PRINTER_CONNECTION = "PRINTER_PRINT_NO_PRINTER_CONNECTION"
}
//# sourceMappingURL=index.d.ts.map