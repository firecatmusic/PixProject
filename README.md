# PixProject
[![Release](https://jitpack.io/v/yzzzd/PixProject.svg)](https://jitpack.io/#yzzzd/PixProject)

Android image picker and camera app based on [PixImagePixer](https://github.com/akshay2211/PixImagePicker/wiki/Documendation-ver-1.5.6) library version 1.5.2 by [Akshay Sharma](https://akshay2211.github.io/).

## Mod
1. CameraX implementation
2. ActivityResultContracts replace ActivityResult

## Download
Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    implementation 'com.github.yzzzd:PixProject:{latest version}'
}
```

## Usage
```kotlin
val options = Options.init()
  .setType(Options.Mode.Both) //Options.Mode.Camera or Options.Mode.Gallery
  .setCount(3)                //Number of images to restict selection count

Pix.open(this, options, activityResultLauncher)
```
or just use with minimal config
```kotlin
Pix.open(this, Options.Mode.Both, activityResultLauncher)
```
for fetching only a single picture.

Use ActivityResultContracts to get results
```kotlin
private var activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == Activity.RESULT_OK) {
    val returnValue = result.data?.getStringArrayListExtra(Pix.IMAGE_RESULTS)
    if (returnValue?.isNotEmpty() == true) {
      val imageFile = File(returnValue[0])
      binding.imageView.setImageURI(imageFile.toUri())
    }
  }
}
```

## License
Licensed under the Apache License, Version 2.0,
