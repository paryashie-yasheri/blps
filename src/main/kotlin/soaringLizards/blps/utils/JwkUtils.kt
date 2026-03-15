import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

object JwkUtils {
    fun generateRsa(): com.nimbusds.jose.jwk.RSAKey? {
        val keyPair = generateRsaKey()
        val publicKey = keyPair.public as RSAPublicKey?
        val privateKey = keyPair.private as RSAPrivateKey?
        return com.nimbusds.jose.jwk.RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
    }

    fun generateRsaKey(): KeyPair {
        val keyPair: KeyPair
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            keyPair = keyPairGenerator.generateKeyPair()
        } catch (ex: Exception) {
            throw IllegalStateException(ex)
        }
        return keyPair
    }
}