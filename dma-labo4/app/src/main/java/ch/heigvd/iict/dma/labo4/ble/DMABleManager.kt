package ch.heigvd.iict.dma.labo4.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import java.util.*

class DMABleManager(applicationContext: Context, private val dmaServiceListener: DMAServiceListener? = null) : BleManager(applicationContext) {

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    fun requestDisconnection() {
        this.disconnect().enqueue()
    }

    private var servicesMap = mutableMapOf(
        timeServiceUUID to timeService,
        symServiceUUID to symService
    )
    private var characteristicsMap = mutableMapOf(
        currentTimeCharUUID to currentTimeChar,
        integerCharUUID to integerChar,
        temperatureCharUUID to temperatureChar,
        buttonClickCharUUID to buttonClickChar
    )
    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {

        Log.d(TAG, "isRequiredServiceSupported - discovered services:")
        for (service in gatt.services) {
            if(!servicesMap.containsKey(service.uuid)) // Treats only specific services
                continue
            Log.d(TAG + "service", service.uuid.toString())

            // Add the service to the services map
            servicesMap[service.uuid] = BluetoothGattService(service.uuid, service.type)

            for (characteristic in service.characteristics) {
                if(!characteristicsMap.containsKey(characteristic.uuid)) // Treats only specific services
                    continue
                Log.d(TAG + "characteristic", characteristic.uuid.toString())

                // Add the characteristic to the characteristics map
                characteristicsMap[characteristic.uuid] = BluetoothGattCharacteristic(characteristic.uuid, characteristic.properties, characteristic.permissions)
            }
        }

        // Check that all services and characteristics are present
        if(servicesMap.containsValue(null) or characteristicsMap.containsValue(null)) {
            Log.e(TAG, "This device doesn't have our use requirement")
            return false
        }

        // Check that each characteristic has the required functionalities
        return (
                // Current time requirements
                hasProperties(currentTimeChar!!, (PROPERTY_NOTIFY or PROPERTY_WRITE)) and
                // Temperature requirements
                hasProperties(temperatureChar!!, PROPERTY_READ) and
                // Integer requirements
                hasProperties(integerChar!!, PROPERTY_WRITE) and
                // Button click requirements
                hasProperties(buttonClickChar!!, PROPERTY_NOTIFY)
                )
    }

    override fun initialize() {
        super.initialize()
        /* TODO
            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE.
            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
            CF. méthodes setNotificationCallback().with{} et enableNotifications().enqueue()
         */
    }

    private fun hasProperties(characteristic: BluetoothGattCharacteristic, requiredProperties: Int) : Boolean {
        return (characteristic.properties and requiredProperties == requiredProperties)
    }

    override fun onServicesInvalidated() {
        super.onServicesInvalidated()
        //we reset services and characteristics
        timeService = null
        currentTimeChar = null
        symService = null
        integerChar = null
        temperatureChar = null
        buttonClickChar = null
    }

    fun readTemperature(): Boolean {
        /* TODO
            on peut effectuer ici la lecture de la caractéristique température
            la valeur récupérée sera envoyée à au ViewModel en utilisant le mécanisme
            du DMAServiceListener: Cf. temperatureUpdate()
                Cf. méthode readCharacteristic().with{}.enqueue()
            On placera des méthodes similaires pour les autres opérations
                Cf. méthode writeCharacteristic().enqueue()
        */

        return false //FIXME
    }

    companion object {
        private val TAG = DMABleManager::class.java.simpleName
        private val timeServiceUUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
        private val symServiceUUID =  UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")
        private val currentTimeCharUUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb")
        private val integerCharUUID = UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f")
        private val temperatureCharUUID = UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f")
        private val buttonClickCharUUID = UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f")
    }

}

interface DMAServiceListener {
    fun dateUpdate(date : Calendar)
    fun temperatureUpdate(temperature : Float)
    fun clickCountUpdate(clickCount : Int)
}
