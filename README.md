## LiteRadar Android transponder, MIT (c) 2022-2025 @miktim [RU](./README-RU.md)

### Purpose
Exchange in a group or transfer to the server of your location via UDP protocol.  

Latest .apk build here: [./app/release/](./app/release/)  

### Requirements  
Android 6+, HarmonyOS 3+  

### Usage  

#### Identification  
The [transponder](https://en.wikipedia.org/wiki/Automatic_Dependent_Surveillance%E2%80%93Broadcast) identifier for data transmission over the network is the public RSA512 key. Authenticity is confirmed by a digital signature of the data packet. For other exchange methods the identifier is a base64 encoded SHA1 hash of the binary representation of the public key.  
The optional name (alias) of the transponder has an auxiliary value.

#### Operating modes
<img
  src="./markdown/settings.png"
  alt="Settings" height=400 width=240/>  
\- tracker only (default). No location data is transmitted to the network.  
\- member of UDP multicast group: 224.0.9.090:9099. Transmitting and receiving geolocation data.  
\- UDP client with specifying IP address and port or available host name and server port. Data transmission only.  

In the last two cases, it is possible to select a network interface.

#### Notifications  
<img
  src="./markdown/notification.png"
  alt="Notification" height=400 width=240/>  
The transponder status is displayed in an Android notification. Location or network error messages are accompanied by a sound signal.  
To access settings or restore app focus, tap the notification text.

#### Interacting with the tracker
The transponder transmits data to the tracker in JSON format.  
Intent Action: "org.literadar.tracker.ACTION"  
Intent extra data: "json"  
Tracker events and responses:  
Intent Action: "org.literadar.tracker.EVENT"  
Intent extra data: "json"  

JSON package structure and tracker management in README  https://github.com/miktim/mini-tracker  

#### Miscellaneous  
Tracker settings are stored in the application's settings.json file.  
In case of a crash, a fatal.log file is created.

### UDP packet structure  
Data is packed in BigEndian order. Double values ​​are converted to IEEE 754 long.  

| Bytes | Contents |
|:----:|------------|
| 4    | magic number "LRdr" |
| 1    | unsigned byte, length of public key in bytes (n) |
| n    | public key |
| 2    | packet version |
| 1    | unsigned byte, length of transponder name (m) |
| m    | UTF-8 transponder name (0:16 chars) |
| 8    | long, timestamp in milliseconds (Epoch from 1 january 1970) |
| 2    | short, location timeout in seconds (> 0) |
| 8    | double, WGS-84 latititude in degrees (-90 : 90) |
| 8    | double, WGS-84 longitude in degrees (-180 : 180) |
| 2    | short, accuracy in meters (> 0)|
| ...  | user data |
| k    | signature |
| 1    | unsigned byte, signature length (k) |
