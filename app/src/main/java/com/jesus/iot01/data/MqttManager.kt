package com.jesus.iot01.data

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager(
    private val onMessageReceived: (topic: String, value: String) -> Unit
) {
    companion object {
        private const val ENDPOINT =
            "ssl://a33qnpsuby70h3-ats.iot.us-east-1.amazonaws.com:8883"
        private const val CLIENT_ID = "android_client_001"
    }

    private var mqttClient: MqttClient? = null

    fun connect(
        caCert: String,
        clientCert: String,
        privateKey: String,
        topics: List<String> = emptyList()
    ) {
        try {
            mqttClient = MqttClient(ENDPOINT, CLIENT_ID, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                socketFactory = SslUtils.getSocketFactory(caCert, clientCert, privateKey)
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Conexión perdida: ${cause?.message}")
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = message.toString()
                    Log.d("MQTT", "Mensaje recibido en $topic: $payload")
                    onMessageReceived(topic, payload)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient?.connect(options)
            Log.d("MQTT", "✅ Conectado a AWS IoT Core")

            //  Suscribirse a todos los tópicos del usuario
            if (topics.isEmpty()) {
                // Si no hay tópicos guardados aún no se suscribe a nada
                Log.d("MQTT", "Sin tópicos para suscribirse")
            } else {
                topics.forEach { topic ->
                    mqttClient?.subscribe(topic, 0)
                    Log.d("MQTT", "✅ Suscrito a $topic")
                }
            }

        } catch (e: Exception) {
            Log.e("MQTT", "❌ Error conectando: ${e.message}")
            throw e
        }
    }

    //  Suscribirse a un tópico nuevo sin reconectar
    fun subscribeToTopic(topic: String) {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.subscribe(topic, 0)
                Log.d("MQTT", "✅ Suscrito dinámicamente a $topic")
            } else {
                Log.e("MQTT", "❌ No conectado — no se puede suscribir a $topic")
            }
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Error suscribiendo a $topic: ${e.message}")
        }
    }

    // ✅ Desuscribirse de un tópico al eliminar un sensor
    fun unsubscribeFromTopic(topic: String) {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.unsubscribe(topic)
                Log.d("MQTT", "✅ Desuscrito de $topic")
            }
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Error desuscribiendo de $topic: ${e.message}")
        }
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d("MQTT", "Desconectado")
        } catch (e: Exception) {
            Log.e("MQTT", "Error desconectando: ${e.message}")
        }
    }
}