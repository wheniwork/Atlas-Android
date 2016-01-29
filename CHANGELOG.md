# Atlas Android

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