const { onCall } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

exports.createPaymentIntent = onCall({
    region: "europe-west1"
}, async (request) => {

    // Ahora leerá la clave directamente de tu archivo .env sin buscar en Secret Manager
    const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);

    if (!request.auth) {
        throw new Error("unauthenticated");
    }

    const amount = request.data.amount;

    try {
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount,
            currency: "eur",
            automatic_payment_methods: {
                enabled: true,
            },
        });

        return {
            clientSecret: paymentIntent.client_secret
        };
    } catch (error) {
        logger.error("Error de Stripe:", error);
        throw new Error("internal");
    }
});