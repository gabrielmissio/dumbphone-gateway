const callbackRepository = require('./infra/repositories/callback-repository')
const whatsappService = require('./services/whatsapp-service')
const { postCommandValidator } = require('./validators/post-command-validator')
const express = require('express');
const app = express();

app.use(express.json());

app.get('/v1/callbacks', async (req, res) => {
    console.log("> GET /callbacks");

    try {
        const callbacks = await callbackRepository.getAll();
        return res.status(200).json({ data: callbacks });
    } catch (error) {
        console.error(error);
        return res.status(500).send({ error: 'Internal server error' });
    }  
});

app.delete('/v1/callbacks/:id', async (req, res) => {
    console.log("> DELETE /callbacks/id");

    try {
        const { id } = req.params;
        await callbackRepository.deleteById(id);
        return res.status(204).send();
    } catch (error) {
        console.error(error);
        return res.status(500).send({ error: 'Internal server error' });
    }  
});

app.post('/v1/command', async (req, res) => {
    console.log("> POST /check");

    try {
        const { error, value } = postCommandValidator.validate(req.body);
        if (error) {
            return res.status(400).json({ error: error.details.map(d => d.message) });
        }

        const { service, payload } = value;
        const handler = await serviceHandlers[service];
        if (!handler) {
            throw new Error('Service not found');
        }

        const result = await handler(payload);

        return res.status(200).json({ data: result })
    } catch (error) {
        console.error(error);
        return res.status(500).send({ error: 'Internal server error' });
    }
});

const serviceHandlers = {
    whatsapp: whatsappService.sendMessage,
    'exchange-rates': () => ({ message: 'Exchange rates service is not implemented yet' }),
    weather: () => ({ message: 'Weather service is not implemented yet' }),
    'ai-assistant': () => ({ message: 'AI assistant service is not implemented yet' }),
};

module.exports = app;
