/**
 * AES-GCM 客户端加密工具
 * 使用 Web Crypto API (SubtleCrypto) 实现
 * 密钥派生：PBKDF2（用户密码 → AES-GCM 密钥）
 * 加密：AES-GCM 256-bit，随机 IV，IV 与密文一起存储
 */

/**
 * 从用户密码派生 AES-GCM 密钥
 * @param {string} password 用户密码
 * @param {Uint8Array} salt 盐值（16字节）
 * @returns {Promise<CryptoKey>}
 */
async function deriveKey(password, salt) {
  const encoder = new TextEncoder()
  const passwordKey = await crypto.subtle.importKey(
    'raw',
    encoder.encode(password),
    'PBKDF2',
    false,
    ['deriveKey']
  )

  return crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: salt,
      iterations: 100000,
      hash: 'SHA-256'
    },
    passwordKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt']
  )
}

/**
 * 加密文本
 * @param {string} plaintext 明文
 * @param {string} password 用户密码
 * @returns {Promise<string>} Base64 编码的加密结果（salt + iv + ciphertext）
 */
export async function encrypt(plaintext, password) {
  const encoder = new TextEncoder()
  const salt = crypto.getRandomValues(new Uint8Array(16))
  const iv = crypto.getRandomValues(new Uint8Array(12))

  const key = await deriveKey(password, salt)

  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: iv },
    key,
    encoder.encode(plaintext)
  )

  // 合并 salt + iv + ciphertext
  const combined = new Uint8Array(salt.length + iv.length + ciphertext.byteLength)
  combined.set(salt, 0)
  combined.set(iv, salt.length)
  combined.set(new Uint8Array(ciphertext), salt.length + iv.length)

  // 返回 Base64 编码
  return btoa(String.fromCharCode(...combined))
}

/**
 * 解密文本
 * @param {string} encryptedData Base64 编码的加密数据
 * @param {string} password 用户密码
 * @returns {Promise<string>} 解密后的明文
 */
export async function decrypt(encryptedData, password) {
  try {
    // Base64 解码
    const combined = Uint8Array.from(atob(encryptedData), c => c.charCodeAt(0))

    // 提取 salt, iv, ciphertext
    const salt = combined.slice(0, 16)
    const iv = combined.slice(16, 28)
    const ciphertext = combined.slice(28)

    const key = await deriveKey(password, salt)

    const plaintext = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: iv },
      key,
      ciphertext
    )

    return new TextDecoder().decode(plaintext)
  } catch (e) {
    throw new Error('解密失败，密钥可能不正确')
  }
}
