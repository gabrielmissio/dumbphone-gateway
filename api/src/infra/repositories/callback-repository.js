const { randomUUID } = require('crypto')

const dumbStorage = []

async function save(callback) {
    if (typeof callback !== 'object') {
        throw new Error('Callback must be an object')
    }

    callback.id = randomUUID()
    console.log('Saving callback', callback)
    return dumbStorage.push(callback)
}

async function getAll() {
    console.log('Getting all callbacks')
    return dumbStorage
}

async function deleteById(id) {
    console.log('Deleting callback', id)
    const index = dumbStorage.findIndex(c => c.id === id)
    if (index === -1) {
        console.log('Callback not found')
        return null
    }
    
    dumbStorage.splice(index, 1)
    return null
}

module.exports = { save, getAll, deleteById };
