package de.bcycle.app

/** One semantic catalogue for sensor identity, across switches, selectors and dashboard labels. */
internal object SensorIcons {
    fun forSensor(sensor:BikeSensor)=when(sensor) {
        BikeSensor.GPS->BicycleIcon.GpsSensor
        BikeSensor.EBIKE->BicycleIcon.EBike
        BikeSensor.HEART->BicycleIcon.Heart
        BikeSensor.POWER->BicycleIcon.PowerMeter
        BikeSensor.TRAINER->BicycleIcon.IndoorBike
    }
    fun forSource(source:String)=when(source) {
        "E-Bike"->BicycleIcon.EBike
        "Indoor-Bike"->BicycleIcon.IndoorBike
        "Akku"->BicycleIcon.Battery
        "Smartphone"->BicycleIcon.Phone
        "GPS"->BicycleIcon.GpsSensor
        "Pulsmesser"->BicycleIcon.Heart
        "Leistung", "Powermeter"->BicycleIcon.PowerMeter
        "Wetter"->BicycleIcon.Weather
        "Light"->BicycleIcon.Light
        "Sunrise"->BicycleIcon.SunriseSensor
        "Sunset"->BicycleIcon.SunsetSensor
        else->BicycleIcon.Bikes
    }
}

/** Bike identity is independent of the currently selected profile. */
internal object BikeTypeIcons {
    fun forType(type: BikeType?) = when(type) {
        BikeType.EBIKE -> BicycleIcon.EBike
        BikeType.INDOOR -> BicycleIcon.IndoorBike
        else -> BicycleIcon.Bikes
    }
}
