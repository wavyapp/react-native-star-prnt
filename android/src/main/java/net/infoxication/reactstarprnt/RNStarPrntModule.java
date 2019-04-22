
package net.infoxication.reactstarprnt;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.IConnectionCallback;
import com.starmicronics.starioextension.IDisplayCommandBuilder;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.Emulation;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction;
import com.starmicronics.starioextension.ICommandBuilder.CodePageType;
import com.starmicronics.starioextension.StarIoExtManager;
import com.starmicronics.starioextension.StarIoExtManagerListener;
import com.starmicronics.starioextension.IPeripheralConnectParser;
import com.starmicronics.stario.StarBluetoothManager;
import com.starmicronics.starioextension.StarBluetoothManagerFactory;

public class RNStarPrntModule extends ReactContextBaseJavaModule {
    private enum PeripheralStatus {
        Invalid, Impossible, Connect, Disconnect,
    }

    private final ReactApplicationContext reactContext;
    private StarIoExtManager starIoExtManager;
    private PeripheralStatus displayStatus = PeripheralStatus.Invalid;
    private StarBluetoothManager mBluetoothManager;
    private StarBluetoothManager.StarBluetoothSecurity mSecurity;
    private boolean                                    mAutoConnect;

    public RNStarPrntModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNStarPrnt";
    }

    @ReactMethod
    public void portDiscovery(final String strInterface, final Promise promise) {
        new Thread(new Runnable() {
            public void run() {

                WritableArray result = new WritableNativeArray();
                try {
                    switch (strInterface) {
                    case "LAN":
                        result = getPortDiscovery("LAN");
                        break;
                    case "Bluetooth":
                        result = getPortDiscovery("Bluetooth");
                        break;
                    case "USB":
                        result = getPortDiscovery("USB");
                        break;
                    default:
                        result = getPortDiscovery("All");
                        break;
                    }

                } catch (StarIOPortException exception) {
                    promise.reject("PORT_DISCOVERY_ERROR", exception);

                } finally {
                    promise.resolve(result);
                }

            }
        }).start();
    }

    @ReactMethod
    public void checkStatus(final String portName, final String emulation, final Promise promise) {
        new Thread(new Runnable() {
            public void run() {

                String portSettings = getPortSettingsOption(emulation);

                StarIOPort port = null;
                try {
                    port = StarIOPort.getPort(portName, portSettings, 10000, getReactApplicationContext());
                    // A sleep is used to get time for the socket to completely open
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    StarPrinterStatus status;
                    Map<String, String> firmwareInformationMap = port.getFirmwareInformation();
                    status = port.retreiveStatus();
                    WritableNativeMap json = new WritableNativeMap();
                    json.putBoolean("offline", status.offline);
                    json.putBoolean("coverOpen", status.coverOpen);
                    json.putBoolean("cutterError", status.cutterError);
                    json.putBoolean("receiptPaperEmpty", status.receiptPaperEmpty);
                    json.putString("ModelName", firmwareInformationMap.get("ModelName"));
                    json.putString("FirmwareVersion", firmwareInformationMap.get("FirmwareVersion"));
                    promise.resolve(json);
                } catch (StarIOPortException e) {
                    promise.reject("CHECK_STATUS_ERROR", e);
                } finally {

                    if (port != null) {
                        try {
                            StarIOPort.releasePort(port);
                        } catch (StarIOPortException e) {
                            promise.reject("CHECK_STATUS_ERROR", e.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    @ReactMethod
    public void connect(final String portName, final String emulation, final Boolean hasBarcodeReader,
            final Promise promise) {
        Context context = getCurrentActivity();

        String portSettings = getPortSettingsOption(emulation);
        if (starIoExtManager != null && starIoExtManager.getPort() != null) {
            starIoExtManager.disconnect(null);
        }
        starIoExtManager = new StarIoExtManager(
                hasBarcodeReader ? StarIoExtManager.Type.WithBarcodeReader : StarIoExtManager.Type.Standard, portName,
                portSettings, 10000, context);
        starIoExtManager.setListener(starIoExtManagerListener);

        try {
            mBluetoothManager = StarBluetoothManagerFactory.getManager(
                    portName,
                    portSettings,
                    10000,
                    emulation
            );
        }
        catch (StarIOPortException e) {
            
        }


        new Thread(new Runnable() {
            public void run() {

                if (starIoExtManager != null)
                    starIoExtManager.connect(new IConnectionCallback() {
                        @Override
                        public void onConnected(ConnectResult connectResult) {
                            if (connectResult == ConnectResult.Success
                                    || connectResult == ConnectResult.AlreadyConnected) {

                                promise.resolve("Printer Connected");

                            } else {
                                promise.reject("CONNECT_ERROR", "Error Connecting to the printer");
                            }
                        }

                        @Override
                        public void onDisconnected() {
                            // Do nothing
                        }
                    });

            }
        }).start();
    }

    @ReactMethod
    public void disconnect(final Promise promise) {
        new Thread(new Runnable() {
            public void run() {
                if (starIoExtManager != null && starIoExtManager.getPort() != null) {
                    starIoExtManager.disconnect(new IConnectionCallback() {
                        @Override
                        public void onConnected(ConnectResult connectResult) {
                            // nothing
                        }

                        @Override
                        public void onDisconnected() {
                            // sendEvent("printerOffline", null);
                            starIoExtManager.setListener(null); // remove the listener?
                            promise.resolve("Printer Disconnected");
                        }
                    });
                } else {
                    promise.resolve("No printers connected");
                }

            }
        }).start();
    }

    @ReactMethod
    public void showPriceIndicator(final String portName, String emulation, final ReadableArray displayCommands,
            final Promise promise) {
        if (starIoExtManager != null) {
            final String portSettings = getPortSettingsOption(emulation);
            final Context context = getCurrentActivity();

            final IPeripheralConnectParser parser = StarIoExt.createDisplayConnectParser(StarIoExt.DisplayModel.SCD222);

            // WIP
            // TODO Créer une react methode isPriceIndicatorConnected et l'utiliser ici
            // plutot
            Communication.parseDoNotCheckCondition(DisplayFragment.class, parser, mPort,
                    new Communication.SendCallback() {
                        @Override
                        public void onStatus(boolean result, Communication.Result communicateResult) {
                            mWaitCallback = false;

                            if (!mIsForeground) {
                                return;
                            }

                            if (communicateResult == Communication.Result.Success) {
                                if (parser.isConnected()) {
                                    if (mDisplayStatus != PeripheralStatus.Connect) {
                                        mComment.clearAnimation();
                                        mComment.setText("");

                                        mDisplayStatus = PeripheralStatus.Connect;
                                    }
                                } else {
                                    if (mDisplayStatus != PeripheralStatus.Disconnect) {
                                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
                                        mComment.startAnimation(animation);
                                        mComment.setTextColor(Color.RED);
                                        mComment.setText("Display Disconnect");

                                        mDisplayStatus = PeripheralStatus.Disconnect;
                                    }
                                }
                            } else {
                                if (mDisplayStatus != PeripheralStatus.Impossible) {
                                    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
                                    mComment.startAnimation(animation);
                                    mComment.setTextColor(Color.RED);
                                    mComment.setText("Printer Impossible");

                                    mDisplayStatus = PeripheralStatus.Impossible;
                                }
                            }
                        }
                    });

            new Thread(new Runnable() {
                public void run() {
                    IDisplayCommandBuilder builder = StarIoExt
                            .createDisplayCommandBuilder(StarIoExt.DisplayModel.SCD222);
                    builder.appendClearScreen();
                    builder.appendCursorMode(IDisplayCommandBuilder.CursorMode.Off);
                    builder.appendHomePosition();
                    builder.appendInternational(IDisplayCommandBuilder.InternationalType.France);
                    builder.appendCodePage(IDisplayCommandBuilder.CodePageType.CP1252);
                    for (int i = 0; i < displayCommands.size(); i++) {
                        ReadableMap command = displayCommands.getMap(i);
                        try {
                            builder.append(command.getString("appendCustomerDisplay").getBytes("UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    byte[] commands = builder.getPassThroughCommands();
                    if (portName == null) {
                        sendCommandsDoNotCheckCondition(commands, starIoExtManager.getPort(), promise);
                    } else {// use StarIOPort
                        sendCommand(context, portName, portSettings, commands, promise);
                    }
                }
            }).start();
        } else {
            promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
        }
    }

    @ReactMethod
    public void cleanCustomerDisplay(final String portName, String emulation, final ReadableArray displayCommands,
            final Promise promise) {
        if (starIoExtManager != null) {
            final String portSettings = getPortSettingsOption(emulation);
            final Context context = getCurrentActivity();

            new Thread(new Runnable() {
                public void run() {
                    IDisplayCommandBuilder builder = StarIoExt
                            .createDisplayCommandBuilder(StarIoExt.DisplayModel.SCD222);
                    for (int i = 0; i < displayCommands.size(); i++) {
                        ReadableMap command = displayCommands.getMap(i);
                        int isCleaning = command.getInt("cleanCustomerDisplay");
                        if (isCleaning == 1) {
                            builder.appendClearScreen();
                        }
                    }
                    byte[] commands = builder.getPassThroughCommands();
                    if (portName == null) {
                        sendCommandsDoNotCheckCondition(commands, starIoExtManager.getPort(), promise);
                    } else {
                        sendCommand(context, portName, portSettings, commands, promise);
                    }
                }

            }).start();
        } else {
            promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
        }
    }

    @ReactMethod
    public void turnCustomerDisplay(final String turnTo, final String portName, String emulation,
            final ReadableArray displayCommands, final Promise promise) {
        if (!turnTo.equals("on") && !turnTo.equals("off")) {
            promise.reject("STARIO_PORT_EXCEPTION", "Bad turnTo parameter");
        }
        if (starIoExtManager != null) {
            final String portSettings = getPortSettingsOption(emulation);
            final Context context = getCurrentActivity();

            new Thread(new Runnable() {
                public void run() {
                    IDisplayCommandBuilder builder = StarIoExt
                            .createDisplayCommandBuilder(StarIoExt.DisplayModel.SCD222);
                    if (turnTo.equals("on")) {
                        builder.appendTurnOn(true);
                    } else {
                        builder.appendTurnOn(false);
                    }
                    byte[] commands = builder.getPassThroughCommands();

                    if (portName == null) {
                        sendCommandsDoNotCheckCondition(commands, starIoExtManager.getPort(), promise);
                    } else {
                        sendCommand(context, portName, portSettings, commands, promise);
                    }
                }
            }).start();
        } else {
            promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
        }
    }

    @ReactMethod
    public void print(final String portName, String emulation, final ReadableArray printCommands,
            final Promise promise) {
        if (starIoExtManager != null) {
            final String portSettings = getPortSettingsOption(emulation);
            final Emulation _emulation = getEmulation(emulation);
            final Context context = getCurrentActivity();

            new Thread(new Runnable() {
                public void run() {

                    ICommandBuilder builder = StarIoExt.createCommandBuilder(_emulation);

                    builder.beginDocument();

                    appendCommands(builder, printCommands, context);

                    builder.endDocument();

                    byte[] commands = builder.getCommands();

                    // use StarIOExtManager
                    if (portName == null) {
                        sendCommand(commands, starIoExtManager.getPort(), promise);
                    } else {// use StarIOPort
                        sendCommand(context, portName, portSettings, commands, promise);
                    }
                }

            }).start();
        } else {
            promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
        }
    }

    @ReactMethod
    public void optimisticPrint(final String portName, String emulation, final ReadableArray printCommands,
            final Promise promise) {
        if (starIoExtManager != null) {
            final String portSettings = getPortSettingsOption(emulation);
            final Emulation _emulation = getEmulation(emulation);
            final Context context = getCurrentActivity();

            new Thread(new Runnable() {
                public void run() {

                    ICommandBuilder builder = StarIoExt.createCommandBuilder(_emulation);

                    builder.beginDocument();

                    appendCommands(builder, printCommands, context);

                    builder.endDocument();

                    byte[] commands = builder.getCommands();

                    if (portName == null) {
                        sendCommandsDoNotCheckCondition(commands, starIoExtManager.getPort(), promise);
                    } else {
                        sendCommand(context, portName, portSettings, commands, promise);
                    }
                }

            }).start();
        } else {
            promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
        }

    }

    private Emulation getEmulation(String emulation) {
        if (emulation == null) {
            return Emulation.StarPRNT;
        }
        if (emulation.equals("StarPRNT"))
            return Emulation.StarPRNT;
        else if (emulation.equals("StarPRNTL"))
            return Emulation.StarPRNTL;
        else if (emulation.equals("StarLine"))
            return Emulation.StarLine;
        else if (emulation.equals("StarGraphic"))
            return Emulation.StarGraphic;
        else if (emulation.equals("EscPos"))
            return Emulation.EscPos;
        else if (emulation.equals("EscPosMobile"))
            return Emulation.EscPosMobile;
        else if (emulation.equals("StarDotImpact"))
            return Emulation.StarDotImpact;
        else
            return Emulation.StarLine;
    }

    private WritableArray getPortDiscovery(String interfaceName) throws StarIOPortException {
        List<PortInfo> BTPortList;
        List<PortInfo> TCPPortList;
        List<PortInfo> USBPortList;

        final ArrayList<PortInfo> arrayDiscovery = new ArrayList<PortInfo>();

        WritableArray arrayPorts = new WritableNativeArray();

        if (interfaceName.equals("Bluetooth") || interfaceName.equals("All")) {
            BTPortList = StarIOPort.searchPrinter("BT:");

            arrayDiscovery.addAll(BTPortList);
        }
        if (interfaceName.equals("LAN") || interfaceName.equals("All")) {
            TCPPortList = StarIOPort.searchPrinter("TCP:");

            arrayDiscovery.addAll(TCPPortList);
        }
        if (interfaceName.equals("USB") || interfaceName.equals("All")) {
            try {
                USBPortList = StarIOPort.searchPrinter("USB:", getReactApplicationContext());
            } catch (StarIOPortException e) {
                USBPortList = new ArrayList<PortInfo>();
            }
            arrayDiscovery.addAll(USBPortList);
        }

        for (PortInfo discovery : arrayDiscovery) {
            WritableMap port = new WritableNativeMap();
            if (discovery.getPortName().startsWith("BT:"))
                port.putString("portName", "BT:" + discovery.getMacAddress());
            else
                port.putString("portName", discovery.getPortName());

            if (!discovery.getMacAddress().equals("")) {

                port.putString("macAddress", discovery.getMacAddress());

                if (discovery.getPortName().startsWith("BT:")) {
                    port.putString("modelName", discovery.getPortName());
                } else if (!discovery.getModelName().equals("")) {
                    port.putString("modelName", discovery.getModelName());
                }
            } else if (interfaceName.equals("USB") || interfaceName.equals("All")) {
                if (!discovery.getModelName().equals("")) {
                    port.putString("modelName", discovery.getModelName());
                }
                if (!discovery.getUSBSerialNumber().equals(" SN:")) {
                    port.putString("USBSerialNumber", discovery.getUSBSerialNumber());
                }
            }

            arrayPorts.pushMap(port);
        }

        return arrayPorts;
    }

    private String getPortSettingsOption(String emulation) { // generate the portsettings depending on the emulation
                                                             // type

        String portSettings = "";
        if (emulation == null) {
            return portSettings;
        }

        switch (emulation) {
        case "EscPosMobile":
            portSettings += "mini";
            break;
        case "EscPos":
            portSettings += "escpos";
            break;
        // StarLine, StarGraphic, StarDotImpact
        case "StarPRNT":
        case "StarPRNTL":
            portSettings += "Portable";
            portSettings += ";l"; // retry on

            break;
        default:
            portSettings += "";
            break;
        }
        return portSettings;
    }

    @ReactMethod
    private boolean optimisticSendCommand(byte[] commands, StarIOPort port, Promise promise) {
        try {
            /*
             * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
             */
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (port == null) { // Not connected or port closed
                promise.reject("STARIO_PORT_EXCEPTION",
                        "Unable to Open Port, Please Connect to the printer before sending commands");
                return false;
            }

            StarPrinterStatus status;
            status = port.beginCheckedBlock();
            if (status.offline) {
                throw new StarIOPortException("A printer is offline");
            }
            port.writePort(commands, 0, commands.length);
            port.setEndCheckedBlockTimeoutMillis(30000);// Change the timeout time of endCheckedBlock method.
            status = port.endCheckedBlock();

            if (status.coverOpen) {
                promise.reject("STARIO_PORT_EXCEPTION", "Cover open");
                // sendEvent("printerCoverOpen", null);
                return false;
            } else if (status.receiptPaperEmpty) {
                promise.reject("STARIO_PORT_EXCEPTION", "Empty paper");
                // sendEvent("printerPaperEmpty", null);
                return false;
            } else if (status.offline) {
                promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
                // sendEvent("printerOffline", null);
                return false;
            }
            promise.resolve("Success!");

        } catch (StarIOPortException e) {
            // sendEvent("printerImpossible", e.getMessage());
            promise.reject("STARIO_PORT_EXCEPTION", e.getMessage());
            return false;
        } finally {
            return true;
        }
    }

    private boolean sendCommand(byte[] commands, StarIOPort port, Promise promise) {

        try {
            /*
             * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
             */
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (port == null) { // Not connected or port closed
                promise.reject("STARIO_PORT_EXCEPTION",
                        "Unable to Open Port, Please Connect to the printer before sending commands");
                return false;
            }

            /*
             * Using Begin / End Checked Block method When sending large amounts of raster
             * data, adjust the value in the timeout in the "StarIOPort.getPort" in order to
             * prevent "timeout" of the "endCheckedBlock method" while a printing.
             *
             * If receipt print is success but timeout error occurs(Show message which is
             * "There was no response of the printer within the timeout period." ), need to
             * change value of timeout more longer in "StarIOPort.getPort" method. (e.g.)
             * 10000 -> 30000
             */
            StarPrinterStatus status;

            status = port.beginCheckedBlock();

            if (status.offline) {
                // sendEvent("printerOffline", null);
                throw new StarIOPortException("A printer is offline");
                // callbackContext.error("The printer is offline");
            }

            port.writePort(commands, 0, commands.length);

            port.setEndCheckedBlockTimeoutMillis(40000);// Change the timeout time of endCheckedBlock method.

            status = port.endCheckedBlock();

            if (status.coverOpen) {
                promise.reject("STARIO_PORT_EXCEPTION", "Cover open");
                // sendEvent("printerCoverOpen", null);
                return false;
            } else if (status.receiptPaperEmpty) {
                promise.reject("STARIO_PORT_EXCEPTION", "Empty paper");
                // sendEvent("printerPaperEmpty", null);
                return false;
            } else if (status.offline) {
                promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
                // sendEvent("printerOffline", null);
                return false;
            }
            promise.resolve("Success!");

        } catch (StarIOPortException e) {
            // sendEvent("printerImpossible", e.getMessage());
            promise.reject("STARIO_PORT_EXCEPTION", e.getMessage());
            return false;
        } finally {
            return true;
        }
    }

    @ReactMethod
    private boolean sendCommandsDoNotCheckCondition(byte[] commands, StarIOPort port, Promise promise) {
        try {
            if (port == null) { // Not connected or port closed
                promise.reject("STARIO_PORT_EXCEPTION",
                        "Unable to Open Port, Please Connect to the printer before sending commands");
                return false;
            }
            port.writePort(commands, 0, commands.length);
            promise.resolve("Success!");
        } catch (StarIOPortException e) {
            sendEvent("printerImpossible", e.getMessage());
            promise.reject("STARIO_PORT_EXCEPTION", e.getMessage());
            return false;
        } finally {
            return true;
        }
    }

    private boolean sendCommand(Context context, String portName, String portSettings, byte[] commands,
            Promise promise) {

        StarIOPort port = null;
        try {
            /*
             * using StarIOPort3.1.jar (support USB Port) Android OS Version: upper 2.2
             */
            port = StarIOPort.getPort(portName, portSettings, 10000, context);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new StarIOPortException(e.toString());
            }

            /*
             * Using Begin / End Checked Block method When sending large amounts of raster
             * data, adjust the value in the timeout in the "StarIOPort.getPort" in order to
             * prevent "timeout" of the "endCheckedBlock method" while a printing.
             *
             * If receipt print is success but timeout error occurs(Show message which is
             * "There was no response of the printer within the timeout period." ), need to
             * change value of timeout more longer in "StarIOPort.getPort" method. (e.g.)
             * 10000 -> 30000
             */
            StarPrinterStatus status = port.beginCheckedBlock();

            if (status.offline) {
                // throw new StarIOPortException("A printer is offline");
                throw new StarIOPortException("A printer is offline");
            }

            port.writePort(commands, 0, commands.length);

            port.setEndCheckedBlockTimeoutMillis(30000);// Change the timeout time of endCheckedBlock method.
            status = port.endCheckedBlock();

            if (status.coverOpen) {
                promise.reject("STARIO_PORT_EXCEPTION", "Cover open");
                return false;
            } else if (status.receiptPaperEmpty) {
                promise.reject("STARIO_PORT_EXCEPTION", "Empty paper");
                return false;
            } else if (status.offline) {
                promise.reject("STARIO_PORT_EXCEPTION", "Printer offline");
                return false;
            }
            promise.resolve("Success!");

        } catch (StarIOPortException e) {
            promise.reject("STARIO_PORT_EXCEPTION", e.getMessage());
        } finally {
            if (port != null) {
                try {
                    StarIOPort.releasePort(port);
                } catch (StarIOPortException e) {
                }
            }
            return true;
        }
    }

    @ReactMethod
    private boolean sendCommandsDoNotCheckCondition(Boolean autoConnectEnabled, Promise promise) {
        if (mBluetoothManager.getAutoConnectCapability() == StarBluetoothManager.StarBluetoothSettingCapability.SUPPORT) {
            mAutoConnect = mBluetoothManager.getAutoConnect();
            mAutoConnectSwitch.setChecked(mAutoConnect);
            mAutoConnectSwitch.setEnabled(true);
        }
        else {
            mAutoConnectSwitch.setText("N/A");
            mAutoConnectSwitch.setEnabled(false);
        }

    }

    private void appendCommands(ICommandBuilder builder, ReadableArray printCommands, Context context) {
        Charset encoding = Charset.forName("US-ASCII");
        for (int i = 0; i < printCommands.size(); i++) {
            ReadableMap command = printCommands.getMap(i);
            if (command.hasKey("appendCharacterSpace"))
                builder.appendCharacterSpace(command.getInt("appendCharacterSpace"));
            else if (command.hasKey("appendEncoding"))
                encoding = getEncoding(command.getString("appendEncoding"));
            else if (command.hasKey("appendCodePage"))
                builder.appendCodePage(getCodePageType(command.getString("appendCodePage")));
            else if (command.hasKey("append"))
                builder.append(command.getString("append").getBytes(encoding));
            else if (command.hasKey("appendRaw"))
                builder.append(command.getString("appendRaw").getBytes(encoding));
            else if (command.hasKey("appendEmphasis"))
                builder.appendEmphasis(command.getString("appendEmphasis").getBytes(encoding));
            else if (command.hasKey("enableEmphasis"))
                builder.appendEmphasis(command.getBoolean("enableEmphasis"));
            else if (command.hasKey("appendInvert"))
                builder.appendInvert(command.getString("appendInvert").getBytes(encoding));
            else if (command.hasKey("enableInvert"))
                builder.appendInvert(command.getBoolean("enableInvert"));
            else if (command.hasKey("appendUnderline"))
                builder.appendUnderLine(command.getString("appendUnderline").getBytes(encoding));
            else if (command.hasKey("enableUnderline"))
                builder.appendUnderLine(command.getBoolean("enableUnderline"));
            else if (command.hasKey("appendInternational"))
                builder.appendInternational(getInternational(command.getString("appendInternational")));
            else if (command.hasKey("appendLineFeed"))
                builder.appendLineFeed(command.getInt("appendLineFeed"));
            else if (command.hasKey("appendUnitFeed"))
                builder.appendUnitFeed(command.getInt("appendUnitFeed"));
            else if (command.hasKey("appendLineSpace"))
                builder.appendLineSpace(command.getInt("appendLineSpace"));
            else if (command.hasKey("appendFontStyle"))
                builder.appendFontStyle(getFontStyle(command.getString("appendFontStyle")));
            else if (command.hasKey("appendCutPaper"))
                builder.appendCutPaper(getCutPaperAction(command.getString("appendCutPaper")));
            else if (command.hasKey("openCashDrawer"))
                builder.appendPeripheral(getPeripheralChannel(command.getInt("openCashDrawer")));
            else if (command.hasKey("appendBlackMark"))
                builder.appendBlackMark(getBlackMarkType(command.getString("appendBlackMark")));
            else if (command.hasKey("appendBytes")) {
                ReadableArray bytesArray = command.getArray("appendBytes");
                if (bytesArray != null) {
                    byte[] byteData = new byte[bytesArray.size() + 1];
                    for (int j = 0; j < bytesArray.size(); j++) {
                        byteData[j] = (byte) bytesArray.getInt(j);
                    }
                    builder.append(byteData);
                }
            } else if (command.hasKey("appendRawBytes")) {
                ReadableArray rawBytesArray = command.getArray("appendRawBytes");
                if (rawBytesArray != null) {
                    byte[] rawByteData = new byte[rawBytesArray.size() + 1];
                    for (int j = 0; j < rawBytesArray.size(); j++) {
                        rawByteData[j] = (byte) rawBytesArray.getInt(j);
                    }
                    builder.appendRaw(rawByteData);
                }
            } else if (command.hasKey("appendAbsolutePosition")) {
                if (command.hasKey("data"))
                    builder.appendAbsolutePosition((command.getString("data").getBytes(encoding)),
                            command.getInt("appendAbsolutePosition"));
                else
                    builder.appendAbsolutePosition(command.getInt("appendAbsolutePosition"));
            } else if (command.hasKey("appendAlignment")) {
                if (command.hasKey("data"))
                    builder.appendAlignment((command.getString("data").getBytes(encoding)),
                            getAlignment(command.getString("appendAlignment")));
                else
                    builder.appendAlignment(getAlignment(command.getString("appendAlignment")));
            } else if (command.hasKey("appendHorizontalTabPosition")) {
                ReadableArray tabPositionsArray = command.getArray("appendHorizontalTabPosition");
                if (tabPositionsArray != null) {
                    int[] tabPositions = new int[tabPositionsArray.size()];
                    for (int j = 0; j < tabPositionsArray.size(); j++) {
                        tabPositions[j] = tabPositionsArray.getInt(j);
                    }
                    builder.appendHorizontalTabPosition(tabPositions);
                }
            } else if (command.hasKey("appendLogo")) {
                ICommandBuilder.LogoSize logoSize = (command.hasKey("logoSize")
                        ? getLogoSize(command.getString("logoSize"))
                        : getLogoSize("Normal"));
                builder.appendLogo(logoSize, command.getInt("appendLogo"));
            } else if (command.hasKey("appendBarcode")) {
                ICommandBuilder.BarcodeSymbology barcodeSymbology = (command.hasKey("BarcodeSymbology")
                        ? getBarcodeSymbology(command.getString("BarcodeSymbology"))
                        : getBarcodeSymbology("Code128"));
                ICommandBuilder.BarcodeWidth barcodeWidth = (command.hasKey("BarcodeWidth")
                        ? getBarcodeWidth(command.getString("BarcodeWidth"))
                        : getBarcodeWidth("Mode2"));
                int height = (command.hasKey("height") ? command.getInt("height") : 40);
                Boolean hri = (!command.hasKey("hri") || command.getBoolean("hri"));
                if (command.hasKey("absolutePosition")) {
                    int position = command.getInt("absolutePosition");
                    builder.appendBarcodeWithAbsolutePosition(command.getString("appendBarcode").getBytes(encoding),
                            barcodeSymbology, barcodeWidth, height, hri, position);
                } else if (command.hasKey("alignment")) {
                    ICommandBuilder.AlignmentPosition alignmentPosition = getAlignment(command.getString("alignment"));
                    builder.appendBarcodeWithAlignment(command.getString("appendBarcode").getBytes(encoding),
                            barcodeSymbology, barcodeWidth, height, hri, alignmentPosition);
                } else
                    builder.appendBarcode(command.getString("appendBarcode").getBytes(encoding), barcodeSymbology,
                            barcodeWidth, height, hri);
            } else if (command.hasKey("appendMultiple")) {
                int width = (command.hasKey("width") ? command.getInt("width") : 1);
                int height = (command.hasKey("height") ? command.getInt("height") : 1);
                builder.appendMultiple(command.getString("appendMultiple").getBytes(encoding), width, height);
            } else if (command.hasKey("enableMultiple")) {
                int width = (command.hasKey("width") ? command.getInt("width") : 1);
                int height = (command.hasKey("height") ? command.getInt("height") : 1);
                Boolean enableMultiple = command.getBoolean("enableMultiple");
                if (enableMultiple)
                    builder.appendMultiple(width, height);
                else
                    builder.appendMultiple(1, 1); // Reset to default when false sent
            } else if (command.hasKey("appendQrCode")) {
                ICommandBuilder.QrCodeModel qrCodeModel = (command.hasKey("QrCodeModel")
                        ? getQrCodeModel(command.getString("QrCodeModel"))
                        : getQrCodeModel("No2"));
                ICommandBuilder.QrCodeLevel qrCodeLevel = (command.hasKey("QrCodeLevel")
                        ? getQrCodeLevel(command.getString("QrCodeLevel"))
                        : getQrCodeLevel("H"));
                int cell = (command.hasKey("cell") ? command.getInt("cell") : 4);
                if (command.hasKey("absolutePosition")) {
                    int position = command.getInt("absolutePosition");
                    builder.appendQrCodeWithAbsolutePosition(command.getString("appendQrCode").getBytes(encoding),
                            qrCodeModel, qrCodeLevel, cell, position);
                } else if (command.hasKey("alignment")) {
                    ICommandBuilder.AlignmentPosition alignmentPosition = getAlignment(command.getString("alignment"));
                    builder.appendQrCodeWithAlignment(command.getString("appendQrCode").getBytes(encoding), qrCodeModel,
                            qrCodeLevel, cell, alignmentPosition);
                } else
                    builder.appendQrCode(command.getString("appendQrCode").getBytes(encoding), qrCodeModel, qrCodeLevel,
                            cell);
            } else if (command.hasKey("appendBitmap")) {
                ContentResolver contentResolver = context.getContentResolver();
                String uriString = command.getString("appendBitmap");
                boolean diffusion = (!command.hasKey("diffusion")) || command.getBoolean("diffusion");
                int width = (command.hasKey("width")) ? command.getInt("width") : 576;
                boolean bothScale = (!command.hasKey("bothScale")) || command.getBoolean("bothScale");
                ICommandBuilder.BitmapConverterRotation rotation = (command.hasKey("rotation"))
                        ? getConverterRotation(command.getString("rotation"))
                        : getConverterRotation("Normal");
                try {
                    Uri imageUri = Uri.parse(uriString);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
                    if (command.hasKey("absolutePosition")) {
                        int position = command.getInt("absolutePosition");
                        builder.appendBitmapWithAbsolutePosition(bitmap, diffusion, width, bothScale, rotation,
                                position);
                    } else if (command.hasKey("alignment")) {
                        ICommandBuilder.AlignmentPosition alignmentPosition = getAlignment(
                                command.getString("alignment"));
                        builder.appendBitmapWithAlignment(bitmap, diffusion, width, bothScale, rotation,
                                alignmentPosition);
                    } else
                        builder.appendBitmap(bitmap, diffusion, width, bothScale, rotation);
                } catch (IOException e) {

                }
            }
        }

    }

    // ICommandBuilder Constant Functions
    private ICommandBuilder.InternationalType getInternational(String international) {
        switch (international) {
        case "UK":
            return ICommandBuilder.InternationalType.UK;
        case "USA":
            return ICommandBuilder.InternationalType.USA;
        case "France":
            return ICommandBuilder.InternationalType.France;
        case "Germany":
            return ICommandBuilder.InternationalType.Germany;
        case "Denmark":
            return ICommandBuilder.InternationalType.Denmark;
        case "Sweden":
            return ICommandBuilder.InternationalType.Sweden;
        case "Italy":
            return ICommandBuilder.InternationalType.Italy;
        case "Spain":
            return ICommandBuilder.InternationalType.Spain;
        case "Japan":
            return ICommandBuilder.InternationalType.Japan;
        case "Norway":
            return ICommandBuilder.InternationalType.Norway;
        case "Denmark2":
            return ICommandBuilder.InternationalType.Denmark2;
        case "Spain2":
            return ICommandBuilder.InternationalType.Spain2;
        case "LatinAmerica":
            return ICommandBuilder.InternationalType.LatinAmerica;
        case "Korea":
            return ICommandBuilder.InternationalType.Korea;
        case "Ireland":
            return ICommandBuilder.InternationalType.Ireland;
        case "Legal":
            return ICommandBuilder.InternationalType.Legal;
        default:
            return ICommandBuilder.InternationalType.USA;
        }
    }

    private ICommandBuilder.AlignmentPosition getAlignment(String alignment) {
        switch (alignment) {
        case "Left":
            return ICommandBuilder.AlignmentPosition.Left;
        case "Center":
            return ICommandBuilder.AlignmentPosition.Center;
        case "Right":
            return ICommandBuilder.AlignmentPosition.Right;
        default:
            return ICommandBuilder.AlignmentPosition.Left;
        }
    }

    private ICommandBuilder.BarcodeSymbology getBarcodeSymbology(String barcodeSymbology) {
        switch (barcodeSymbology) {
        case "Code128":
            return ICommandBuilder.BarcodeSymbology.Code128;
        case "Code39":
            return ICommandBuilder.BarcodeSymbology.Code39;
        case "Code93":
            return ICommandBuilder.BarcodeSymbology.Code93;
        case "ITF":
            return ICommandBuilder.BarcodeSymbology.ITF;
        case "JAN8":
            return ICommandBuilder.BarcodeSymbology.JAN8;
        case "JAN13":
            return ICommandBuilder.BarcodeSymbology.JAN13;
        case "NW7":
            return ICommandBuilder.BarcodeSymbology.NW7;
        case "UPCA":
            return ICommandBuilder.BarcodeSymbology.UPCA;
        case "UPCE":
            return ICommandBuilder.BarcodeSymbology.UPCE;
        default:
            return ICommandBuilder.BarcodeSymbology.Code128;
        }
    }

    private ICommandBuilder.BarcodeWidth getBarcodeWidth(String barcodeWidth) {
        if (barcodeWidth.equals("Mode1"))
            return ICommandBuilder.BarcodeWidth.Mode1;
        if (barcodeWidth.equals("Mode2"))
            return ICommandBuilder.BarcodeWidth.Mode2;
        if (barcodeWidth.equals("Mode3"))
            return ICommandBuilder.BarcodeWidth.Mode3;
        if (barcodeWidth.equals("Mode4"))
            return ICommandBuilder.BarcodeWidth.Mode4;
        if (barcodeWidth.equals("Mode5"))
            return ICommandBuilder.BarcodeWidth.Mode5;
        if (barcodeWidth.equals("Mode6"))
            return ICommandBuilder.BarcodeWidth.Mode6;
        if (barcodeWidth.equals("Mode7"))
            return ICommandBuilder.BarcodeWidth.Mode7;
        if (barcodeWidth.equals("Mode8"))
            return ICommandBuilder.BarcodeWidth.Mode8;
        if (barcodeWidth.equals("Mode9"))
            return ICommandBuilder.BarcodeWidth.Mode9;
        return ICommandBuilder.BarcodeWidth.Mode2;
    }

    private ICommandBuilder.FontStyleType getFontStyle(String fontStyle) {
        if (fontStyle.equals("A"))
            return ICommandBuilder.FontStyleType.A;
        if (fontStyle.equals("B"))
            return ICommandBuilder.FontStyleType.B;
        return ICommandBuilder.FontStyleType.A;
    }

    private ICommandBuilder.LogoSize getLogoSize(String logoSize) {
        switch (logoSize) {
        case "Normal":
            return ICommandBuilder.LogoSize.Normal;
        case "DoubleWidth":
            return ICommandBuilder.LogoSize.DoubleWidth;
        case "DoubleHeight":
            return ICommandBuilder.LogoSize.DoubleHeight;
        case "DoubleWidthDoubleHeight":
            return ICommandBuilder.LogoSize.DoubleWidthDoubleHeight;
        default:
            return ICommandBuilder.LogoSize.Normal;
        }
    }

    private ICommandBuilder.CutPaperAction getCutPaperAction(String cutPaperAction) {
        switch (cutPaperAction) {
        case "FullCut":
            return CutPaperAction.FullCut;
        case "FullCutWithFeed":
            return CutPaperAction.FullCutWithFeed;
        case "PartialCut":
            return CutPaperAction.PartialCut;
        case "PartialCutWithFeed":
            return CutPaperAction.PartialCutWithFeed;
        default:
            return CutPaperAction.PartialCutWithFeed;
        }
    }

    private ICommandBuilder.PeripheralChannel getPeripheralChannel(int peripheralChannel) {
        if (peripheralChannel == 1)
            return ICommandBuilder.PeripheralChannel.No1;
        else if (peripheralChannel == 2)
            return ICommandBuilder.PeripheralChannel.No2;
        else
            return ICommandBuilder.PeripheralChannel.No1;
    }

    private ICommandBuilder.QrCodeModel getQrCodeModel(String qrCodeModel) {
        switch (qrCodeModel) {
        case "No1":
            return ICommandBuilder.QrCodeModel.No1;
        case "No2":
            return ICommandBuilder.QrCodeModel.No2;
        default:
            return ICommandBuilder.QrCodeModel.No1;
        }
    }

    private ICommandBuilder.QrCodeLevel getQrCodeLevel(String qrCodeLevel) {
        switch (qrCodeLevel) {
        case "H":
            return ICommandBuilder.QrCodeLevel.H;
        case "L":
            return ICommandBuilder.QrCodeLevel.L;
        case "M":
            return ICommandBuilder.QrCodeLevel.M;
        case "Q":
            return ICommandBuilder.QrCodeLevel.Q;
        default:
            return ICommandBuilder.QrCodeLevel.H;
        }
    }

    private ICommandBuilder.BitmapConverterRotation getConverterRotation(String converterRotation) {
        switch (converterRotation) {
        case "Normal":
            return ICommandBuilder.BitmapConverterRotation.Normal;
        case "Left90":
            return ICommandBuilder.BitmapConverterRotation.Left90;
        case "Right90":
            return ICommandBuilder.BitmapConverterRotation.Right90;
        case "Rotate180":
            return ICommandBuilder.BitmapConverterRotation.Rotate180;
        default:
            return ICommandBuilder.BitmapConverterRotation.Normal;
        }
    }

    private ICommandBuilder.BlackMarkType getBlackMarkType(String blackMarkType) {
        switch (blackMarkType) {
        case "Valid":
            return ICommandBuilder.BlackMarkType.Valid;
        case "Invalid":
            return ICommandBuilder.BlackMarkType.Invalid;
        case "ValidWithDetection":
            return ICommandBuilder.BlackMarkType.ValidWithDetection;
        default:
            return ICommandBuilder.BlackMarkType.Valid;
        }
    }

    private ICommandBuilder.CodePageType getCodePageType(String codePageType) {
        switch (codePageType) {
        case "CP437":
            return CodePageType.CP437;
        case "CP737":
            return CodePageType.CP737;
        case "CP772":
            return CodePageType.CP772;
        case "CP774":
            return CodePageType.CP774;
        case "CP851":
            return CodePageType.CP851;
        case "CP852":
            return CodePageType.CP852;
        case "CP855":
            return CodePageType.CP855;
        case "CP857":
            return CodePageType.CP857;
        case "CP858":
            return CodePageType.CP858;
        case "CP860":
            return CodePageType.CP860;
        case "CP861":
            return CodePageType.CP861;
        case "CP862":
            return CodePageType.CP862;
        case "CP863":
            return CodePageType.CP863;
        case "CP864":
            return CodePageType.CP864;
        case "CP865":
            return CodePageType.CP866;
        case "CP869":
            return CodePageType.CP869;
        case "CP874":
            return CodePageType.CP874;
        case "CP928":
            return CodePageType.CP928;
        case "CP932":
            return CodePageType.CP932;
        case "CP999":
            return CodePageType.CP999;
        case "CP1001":
            return CodePageType.CP1001;
        case "CP1250":
            return CodePageType.CP1250;
        case "CP1251":
            return CodePageType.CP1251;
        case "CP1252":
            return CodePageType.CP1252;
        case "CP2001":
            return CodePageType.CP2001;
        case "CP3001":
            return CodePageType.CP3001;
        case "CP3002":
            return CodePageType.CP3002;
        case "CP3011":
            return CodePageType.CP3011;
        case "CP3012":
            return CodePageType.CP3012;
        case "CP3021":
            return CodePageType.CP3021;
        case "CP3041":
            return CodePageType.CP3041;
        case "CP3840":
            return CodePageType.CP3840;
        case "CP3841":
            return CodePageType.CP3841;
        case "CP3843":
            return CodePageType.CP3843;
        case "CP3845":
            return CodePageType.CP3845;
        case "CP3846":
            return CodePageType.CP3846;
        case "CP3847":
            return CodePageType.CP3847;
        case "CP3848":
            return CodePageType.CP3848;
        case "UTF8":
            return CodePageType.UTF8;
        case "Blank":
            return CodePageType.Blank;
        default:
            return CodePageType.CP998;
        }
    }

    // Helper functions

    private Charset getEncoding(String encoding) {

        switch (encoding) {
        case "US-ASCII":
            return Charset.forName("US-ASCII"); // English
        case "Windows-1252":
            try {
                return Charset.forName("Windows-1252"); // French, German, Portuguese, Spanish
            } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                return Charset.forName("UTF-8");
            }
        case "Shift-JIS":
            try {
                return Charset.forName("Shift-JIS"); // Japanese
            } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                return Charset.forName("UTF-8");
            }
        case "Windows-1251":
            try {
                return Charset.forName("Windows-1251"); // Russian
            } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                return Charset.forName("UTF-8");
            }
        case "GB2312":
            try {
                return Charset.forName("GB2312"); // Simplified Chinese
            } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                return Charset.forName("UTF-8");
            }
        case "Big5":
            try {
                return Charset.forName("Big5"); // Traditional Chinese
            } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                return Charset.forName("UTF-8");
            }
        case "UTF-8":
            return Charset.forName("UTF-8"); // UTF-8
        default:
            return Charset.forName("US-ASCII");
        }
    }

    private void sendEvent(String dataType, String info) {
        ReactContext reactContext = getReactApplicationContext();
        String eventName = "starPrntData";
        WritableMap params = new WritableNativeMap();
        params.putString("dataType", dataType);
        if (info != null)
            params.putString("data", info);
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    private StarIoExtManagerListener starIoExtManagerListener = new StarIoExtManagerListener() {
        @Override
        public void onPrinterImpossible() {
            sendEvent("printerImpossible", null);
        }

        @Override
        public void onPrinterOnline() {
            sendEvent("printerOnline", null);
        }

        @Override
        public void onPrinterOffline() {
            sendEvent("printerOffline", null);
        }

        @Override
        public void onPrinterPaperReady() {
            sendEvent("printerPaperReady", null);
        }

        @Override
        public void onPrinterPaperNearEmpty() {
            sendEvent("printerPaperNearEmpty", null);
        }

        @Override
        public void onPrinterPaperEmpty() {
            sendEvent("printerPaperEmpty", null);
        }

        @Override
        public void onPrinterCoverOpen() {
            sendEvent("printerCoverOpen", null);
        }

        @Override
        public void onPrinterCoverClose() {
            sendEvent("printerCoverClose", null);
        }

        // Cash Drawer events
        @Override
        public void onCashDrawerOpen() {
            sendEvent("cashDrawerOpen", null);
        }

        @Override
        public void onCashDrawerClose() {
            sendEvent("cashDrawerClose", null);
        }

        @Override
        public void onBarcodeReaderImpossible() {
            sendEvent("barcodeReaderImpossible", null);
        }

        @Override
        public void onBarcodeReaderConnect() {
            sendEvent("barcodeReaderConnect", null);
        }

        @Override
        public void onBarcodeReaderDisconnect() {
            sendEvent("barcodeReaderDisconnect", null);
        }

        @Override
        public void onBarcodeDataReceive(byte[] data) {
            sendEvent("barcodeDataReceive", new String(data));
        }
    };

}
