# @wavyapp/react-native-star-prnt

react-native bridge for [Star micronics printers](http://www.starmicronics.com/pages/All-Products).

## Installation

`$ yarn add @wavyapp/react-native-star-prnt`

#### iOS Configuration

1. In XCode, go to Build Phases, Link Binary with Libraries and Add the following frameworks:
    - Add the `CoreBluetooth.framework`
    - Add the `ExternalAccessory.framework`

#### For Bluetooth printers:

1. Click on the information property list file (default : “Info.plist”).
2. Add the “Supported external accessory protocols” Key.
3. Click the triangle of this key and set the value for the `Item 0` to `jp.star-m.starpro`

#### When using Bluetooth Low Energy printer on iOS13 or later
  1. Click on the information property list file (default : “Info.plist”).
  2. Add the Privacy – Bluetooth Always Usage Description Key.
  3. Set the reason for using Bluetooth in Value (e.g. Use Bluetooth for communication with the printer. )
  4. When communicating with the Bluetooth Low Energy printer on iOS13 or higher, an alert requesting permission to access Bluetooth is displayed. The string set in Value is displayed in the alert as the reason for using Bluetooth.

## Usage
```javascript
import { StarPRNT } from '@wavyapp/react-native-star-prnt';

async function portDiscovery() {
    try {
      let printers = await StarPRNT.portDiscovery('All');
      console.log(printers);
    } catch (e) {
      console.error(e);
    }
  }

```
  
## Take a look at the [Documentation](/Documentation.md)