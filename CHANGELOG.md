# Atlas Android

## 0.2.12

### Major Changes
  * Update to Layer Android SDK Version 0.21.2
  
## 0.2.11

### Major Changes
  * Renamed `Log.setAlwaysLoggable` to `Log.setLoggingEnabled`
  * Updated to Layer Android SDK Version 0.21.1

### Bug Fixes
  * A `ViewPager` can now contain `AtlasConversationsRecyclerView` without a manual `refresh()`
    call (APPS-2444)

## 0.2.10

### Major Changes
  * Updated to Layer Android SDK Version 0.21.0
  * Removed `Util.waitForContent` as that is now supported in the Layer SDK
  * Publishing AAR so it can be included via Maven

### Features
  * Allowing customization of attachment menu background via `attachmentSendersBackground`

## 0.2.9

### Major Changes
  * Updated to Layer Android SDK Version 0.20.4

## 0.2.8

### Major Changes
  * Updated to Layer Android SDK Version 0.20.3

## 0.2.7

### Major Changes
  * Updated to Layer Android SDK Version 0.20.2

## 0.2.6

### Major Changes
  * Updated to Layer Android SDK Version 0.20.1

## 0.2.5

### Features
  * Updated to Layer Android SDK Version 0.20.0 with support for `ALL_MY_DEVICES` deletion.
  * Remove requirement for camera permission

## 0.2.4

### Features
  * Updated to Android API 23 for `compileSdkVersion` and `targetSdkVersion`.
  * Added dynamic permission handling to AttachmentSenders.


## 0.2.3

### Features
  * Added styling through XML attributes ([issue #28](https://github.com/layerhq/Atlas-Android/issues/28)).


## 0.2.2

### Features
  * Added `ContentLoadingProgressBar` for image and location cells, as well as the image popup ([issue #32](https://github.com/layerhq/Atlas-Android/issues/32)).
  * `TextCellFactory` parses text for clickable links, emails, addresses, and phone numbers.


## 0.2.1

### Features
  * Added `MessageSender.Callback` for receiving events when sending `Messages` ([issue #33](https://github.com/layerhq/Atlas-Android/issues/33)).
  * Added `AtlasMessageComposer.setMessageSenderCallback(Callback)` for handling sender callbacks in
    aggregate.


## 0.2.0

`0.2.0` was a complete rewrite of the initial Atlas Android preview.  The `0.2.0` APIs are expected to be stable.

### Major Changes
  * Messages are rendered by [AtlasCellFactories](https://github.com/layerhq/Atlas-Android/blob/master/layer-atlas/src/main/java/com/layer/atlas/messagetypes/AtlasCellFactory.java).
  * Messages are sent by [MessageSenders](https://github.com/layerhq/Atlas-Android/blob/master/layer-atlas/src/main/java/com/layer/atlas/messagetypes/MessageSender.java).
  * [Picasso](https://github.com/square/picasso) is now used for image caching and manipulating instead of the Atlas.ImageLoader class.


## 0.1.0

Initial Atlas Android preview.