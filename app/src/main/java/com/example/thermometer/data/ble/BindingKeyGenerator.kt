package com.example.thermometer.data.ble

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Generates binding key for Xiaomi BLE devices using AES-CMAC algorithm.
 *
 * The binding process:
 * 1. Get a random token from the device (4 bytes)
 * 2. Append 8 zero bytes to form 12-byte input
 * 3. Calculate AES-CMAC with the token as key
 * 4. The first 8 bytes of the result is the binding key
 */
object BindingKeyGenerator {

    private const val AES_ALGORITHM = "AES"
    private const val AES_CIPHER = "AES/ECB/NoPadding"

    /**
     * Generate binding key from device token.
     *
     * @param token 4-byte token received from the device
     * @return 16-byte binding key
     */
    fun generateBindingKey(token: ByteArray): ByteArray {
        require(token.size == 4) { "Token must be 4 bytes" }

        // Create the CMAC key from token (4 bytes) + 8 zero bytes = 12 bytes,
        // padded to 16 bytes with 4 zero bytes
        val cmacKey = ByteArray(16)
        System.arraycopy(token, 0, cmacKey, 0, 4)
        // remaining 12 bytes are already zero

        // Input: token (4 bytes) + 8 zero bytes
        val input = ByteArray(12)
        System.arraycopy(token, 0, input, 0, 4)

        return aesCmac(cmacKey, input)
    }

    /**
     * AES-CMAC algorithm implementation (RFC 4493).
     */
    private fun aesCmac(key: ByteArray, message: ByteArray): ByteArray {
        // Generate subkeys
        val subkeys = generateSubkeys(key)

        val n = if (message.isEmpty()) 1 else (message.size + 15) / 16
        val blockCount = message.size / 16
        val isComplete = message.isNotEmpty() && message.size % 16 == 0

        var lastBlock: ByteArray
        if (isComplete) {
            lastBlock = xorBlocks(
                message.copyOfRange((n - 1) * 16, n * 16),
                subkeys.first
            )
        } else {
            val padded = ByteArray(16)
            val offset = (n - 1) * 16
            val remaining = message.size - offset
            System.arraycopy(message, offset, padded, 0, remaining)
            padded[remaining] = 0x80.toByte()
            lastBlock = xorBlocks(padded, subkeys.second)
        }

        var mac = ByteArray(16)
        for (i in 0 until n - 1) {
            val block = message.copyOfRange(i * 16, (i + 1) * 16)
            mac = aesEncrypt(key, xorBlocks(mac, block))
        }
        mac = aesEncrypt(key, xorBlocks(mac, lastBlock))

        return mac
    }

    private fun generateSubkeys(key: ByteArray): Pair<ByteArray, ByteArray> {
        val rb = ByteArray(16)
        rb[15] = 0x87.toByte()

        val l = aesEncrypt(key, ByteArray(16))

        val k1 = leftShift(l)
        if ((l[0].toInt() and 0x80) != 0) {
            k1[15] = (k1[15].toInt() xor rb[15].toInt()).toByte()
        }

        val k2 = leftShift(k1)
        if ((k1[0].toInt() and 0x80) != 0) {
            k2[15] = (k2[15].toInt() xor rb[15].toInt()).toByte()
        }

        return Pair(k1, k2)
    }

    private fun aesEncrypt(key: ByteArray, input: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_CIPHER)
        val secretKey = SecretKeySpec(key, AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(input)
    }

    private fun xorBlocks(a: ByteArray, b: ByteArray): ByteArray {
        val result = ByteArray(a.size)
        for (i in a.indices) {
            result[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        }
        return result
    }

    private fun leftShift(input: ByteArray): ByteArray {
        val result = ByteArray(input.size)
        var overflow = 0
        for (i in input.indices.reversed()) {
            result[i] = ((input[i].toInt() shl 1) or overflow).toByte()
            overflow = if (input[i].toInt() and 0x80 != 0) 1 else 0
        }
        return result
    }
}
