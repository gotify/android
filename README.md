# Alertify Android 

## This fork of Gotify Android adds Spoofing of WEA Messages at the API 29 UI level

## Gotify Android 
<img align="right" src="app.gif" width="250" />

Alertify Android connects to [gotify/server](https://github.com/gotify/server) and shows push notifications on new messages.

## Sening an spoofed alert

### Priority
| Notification | Gotify Priority|
|- |-|
| - | 0 |
| Icon in notification bar | 1 - 3 |
| Icon in notification bar + Sound | 4 - 7 |
| Icon in notification bar + Sound + Vibration | 8 - 10 |
| Wireless Emergency Alert Dialog | 90 - 99 |
| Wireless Emergency Alert Dialog + Sound + Overide volume and DND| 100 - 110 |

### Title options

The title field is used to send the message type, Types are as follows:

*Note, if title is absent or dosent match it defaults to "Critical Alert"*
*Title is case insesitive*

#### CMAS Alerts

| Type | Title Value | Message Type                  | Type | Title Value | Message Type                     |
|------|-------------|-------------------------------|------|-------------|----------------------------------|
| CMAS | President   | Presidental alert             | ETWS | Tsunami     | ETWS Tsunami alert               |
| CMAS | Extreme     | Extreme alert                 | ETWS | Earthquake  | ETWS Earthquake alert            |
| CMAS | Severe      | Severe alert                  | ETWS | ET          | ETWS Earthquake & Tsunami  alert |
| CMAS | Amber       | Amber / Child Abduction alert | ETWS | ETWS        | ETWS Other Message               |
| CMAS | Public      | Public Safety alert           | ETWS | ETWSTest    | ETWS Test Message                |
| CMAS | RMT         | Required Monthly Test         |      |             |                                  |
| CMAS | StateTest   | Local/State Test              |      |             |                                  |
| CMAS | Broadcast   | Broadcast Operator alert      |      |             |                                  |
| CMAS |\<OTHER\>    | Critical alert                |      |             |                                  |


#### ETWS Alerts
| Title Value | Message Type                     |
|-------------|----------------------------------|
| Tsunami     | ETWS Tsunami alert               |
| Earthquake  | ETWS Earthquake alert            |
| ET          | ETWS Earthquake & Tsunami  alert |
| ETWS        | ETWS Other Message               |
| ETWSTest    | ETWS Test Message                |


### Example Commands

**Example CMAS Presidental alert**

`curl "https://<gotifyURL>/message?token=<APPTOKEN>" -F "title=President" -F "message=This is the body" -F "priority=105"`

**Example CMAS Extreme alert muted**

`curl "https://<gotifyURL>/message?token=<APPTOKEN>"-F "title=Extreme" -F "message=This is the body" -F "priority=95"`

**Example ETWS Tsunami alert**

`curl "https://<gotifyURL>/message?token=<APPTOKEN>"" -F "title=Tsunami" -F "message=This is the body" -F "priority=105"`

### Examples

See Example Messages [Images](./Images.md)

![Images](./img/image.png)

## Features

* show push notifications on new messages
* view and delete messages

## Installation

Download the apk or build via Android studio.

### Disable battery optimization

By default Android kills long running apps as they drain the battery. With enabled battery optimization, Gotify will be killed and you wont receive any notifications.

Here is one way to disable battery optimization for Gotify.

* Open "Settings"
* Search for "Battery Optimization"
* Find "Gotify" and disable battery optimization

### Minimize the Gotify foreground notification

*Only possible for Android version >= 8*

The foreground notification with content like `Listening to https://push.yourdomain.eu` can be manually minimized to be less intrusive:

* Open Settings -> Apps -> Gotify
* Click Notifications
* Click on `Gotify foreground notification`
* Select a different "Behavior" or "Importance" (depends on your android version)
* Restart Gotify

## Message Priorities

| Notification | Gotify Priority|
|- |-|
| - | 0 |
| Icon in notification bar | 1 - 3 |
| Icon in notification bar + Sound | 4 - 7 |
| Icon in notification bar + Sound + Vibration | 8 - 10 |
| Wireless Emergency Alert Dialog | 90 - 99 |
| Wireless Emergency Alert Dialog + Sound | 100 - 110 |

## Building

Execute the following command to build the apk.
```bash
$ ./gradlew build
```

## Update client

* Run `./gradlew generateSwaggerCode`
* Discard changes to `client/build.gradle` (newer versions of dependencies)
* Fix compile error in `client/src/main/java/com/github/gotify/client/auth/OAuthOkHttpClient.java` (caused by an updated dependency)
* Delete `client/settings.gradle` (client is a gradle sub project and must not have a settings.gradle)
* Commit changes

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

