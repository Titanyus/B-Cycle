package de.bcycle.app

/** One semantic catalogue for sensor identity, across switches, selectors and dashboard labels. */
internal object SensorIcons {
    fun forSensor(sensor:BikeSensor)=when(sensor) {
        BikeSensor.GPS->BicycleIcon.GpsSensor
        BikeSensor.EBIKE->BicycleIcon.Bikes
        BikeSensor.HEART->BicycleIcon.Heart
        BikeSensor.POWER->BicycleIcon.PowerMeter
        BikeSensor.TRAINER->BicycleIcon.Bikes
    }
    fun forSource(source:String)=when(source) {
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
