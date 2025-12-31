package ma.projet.soapclient.ws

import ma.projet.soapclient.beans.Compte
import ma.projet.soapclient.beans.TypeCompte
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.text.SimpleDateFormat
import java.util.*

class Service {
    private val NAMESPACE = "http://ws.demo.example.com"
    private val URL = "http://10.0.2.2:8082/services/ws"
    private val METHOD_GET_COMPTES = "getComptes"
    private val METHOD_CREATE_COMPTE = "createCompte"
    private val METHOD_DELETE_COMPTE = "deleteCompte"


    fun getComptes(): List<Compte> {
        val request = SoapObject(NAMESPACE, METHOD_GET_COMPTES)
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL)
        val comptes = mutableListOf<Compte>()

        try {
            transport.call("", envelope)
            val response = envelope.bodyIn as SoapObject
            for (i in 0 until response.propertyCount) {
                val soapCompte = response.getProperty(i) as SoapObject
                
                val typeStr = soapCompte.getPropertySafelyAsString("type")
                val typeEnum = try {
                    if (typeStr.isNotEmpty()) TypeCompte.valueOf(typeStr) else TypeCompte.COURANT
                } catch (e: Exception) {
                    TypeCompte.COURANT // Valeur par défaut si parsing échoue
                }

                val dateStr = soapCompte.getPropertySafelyAsString("dateCreation")
                val dateVal = try {
                    // Format standard XML (JAXB) souvent retourné
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr)
                } catch (e: Exception) {
                    try {
                        // Fallback format court
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                    } catch (e2: Exception) {
                        Date()
                    }
                }

                val compte = Compte(
                    id = soapCompte.getPropertySafelyAsString("id")?.toLongOrNull(),
                    solde = soapCompte.getPropertySafelyAsString("solde")?.toDoubleOrNull() ?: 0.0,
                    dateCreation = dateVal ?: Date(),
                    type = typeEnum
                )
                comptes.add(compte)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return comptes
    }

    /**
     * Crée un nouveau compte via le service SOAP.
     * @param solde Solde initial du compte.
     * @param type Type du compte (COURANT ou EPARGNE).
     */
    fun createCompte(solde: Double, type: TypeCompte): Boolean {
        val request = SoapObject(NAMESPACE, METHOD_CREATE_COMPTE).apply {
            // On envoie en String pour éviter l'erreur "Cannot serialize" de ksoap2
            // On utilise arg0 et arg1 car c'est la norme JAX-WS par défaut
            addProperty("solde", solde.toString())
            addProperty("type", type.name)
        }
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL)

        transport.call("", envelope)
        return true
    }

    /**
     * Supprime un compte en fonction de son ID via le service SOAP.
     * @param id Identifiant du compte à supprimer.
     */
    fun deleteCompte(id: Long): Boolean {
        val request = SoapObject(NAMESPACE, METHOD_DELETE_COMPTE).apply {
            addProperty("id", id)
        }
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL)

        return try {
            transport.call("", envelope)
            true // Souvent le delete ne retourne rien ou un succès simple
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Extension pour récupérer une propriété SOAP en toute sécurité
    private fun SoapObject.getPropertySafelyAsString(name: String): String {
        return try {
            if (hasProperty(name)) getProperty(name).toString() else ""
        } catch (e: Exception) {
            ""
        }
    }
}