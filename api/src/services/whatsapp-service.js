const { Client, LocalAuth } = require('whatsapp-web.js')
const qrcode = require('qrcode-terminal')
const callbackRepository = require('../infra/repositories/callback-repository')

let connected = false
const waClient = new Client({
    authStrategy: new LocalAuth(),
})

waClient.initialize()

waClient.on('qr', (qr) => {
  qrcode.generate(qr, { small: true })
})

waClient.on('ready', () => {
    console.log('Client is ready!')
    connected = true
})

waClient.on('message', async (message) => {
    console.log('New message:', message.body)

    const callback = {
        origin: 'whatsapp',
        payload: {
            phoneNumber: message.from,
            message: message.body,
        },
    }

    await callbackRepository.save(callback)
})
    
async function sendMessage({ phoneNumber, message } = {}) {
    if (!connected) {
        throw new Error('Whatsapp client is not connected')
    }
    if (!phoneNumber || !message) {
        throw new Error('phoneNumber and message are required')
    }

    const phoneNumberDetails = await waClient.getNumberId(
        phoneNumber.replace('+', '')
    )
    if (!phoneNumberDetails) {
        throw new Error(`${phoneNumber} is not registered`)
    }
    
    return waClient.sendMessage(phoneNumberDetails._serialized, message)
}

module.exports = { sendMessage }
