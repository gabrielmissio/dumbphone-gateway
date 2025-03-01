const Joi = require('joi');
const ALLOWED_SERVICES = ['whatsapp', 'exchange-rates', 'weather', 'ai-assistant'];

const payloadSchemas = {
    whatsapp: Joi.object({
        phoneNumber: Joi.string().pattern(/^\+\d{1,15}$/).required(),
        message: Joi.string().min(1).max(1000).required(),
    }),
    'exchange-rates': Joi.object({
        baseCurrency: Joi.string().length(3).uppercase().required(),
        targetCurrency: Joi.string().length(3).uppercase().required(),
    }),
    weather: Joi.object({
        location: Joi.string().min(1).max(100).required(),
        date: Joi.string().pattern(/^\d{4}-\d{2}-\d{2}$/).required(),
    }),
    'ai-assistant': Joi.object({
        prompt: Joi.string().min(1).max(5000).required(),
        model: Joi.string().valid('gpt-3.5', 'gpt-4').default('gpt-4'),
    }),
};

const postCommandValidator = Joi.object({
    service: Joi.string().valid(...ALLOWED_SERVICES).required(),
    payload: Joi.alternatives()
        .conditional('service', {
            switch: ALLOWED_SERVICES.map(service => ({
                is: service,
                then: payloadSchemas[service],
            })),
            otherwise: Joi.object().required(),
        }).required(),
});

module.exports = { postCommandValidator };
