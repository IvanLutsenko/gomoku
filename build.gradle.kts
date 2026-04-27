import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
	id = "com.awac.gomoku"
    icon = file("resources/icon.png")
    androidAppendBuildGradle("android.defaultConfig.multiDexEnabled true\n")

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	targetJs()
    targetWasmJs()
	targetIos()
	targetAndroid()

	serializationJson()

    // Для NativeVibration на Android.
    androidPermission("android.permission.VIBRATE")
}


dependencies {
    add("commonMainApi", project(":deps"))
    //add("commonMainApi", project(":korge-dragonbones"))
}

