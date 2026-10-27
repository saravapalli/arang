package com.arang.intake.whatsapp;

/**
 * Transport for outbound WhatsApp messages (REQ-08). Implementations either call the Cloud API
 * (when credentials are configured) or log/capture the message (offline/dev/test). The reply
 * <em>text</em> is built elsewhere ({@code ReplyComposer}); this only sends a string to a recipient.
 */
public interface WhatsAppClient {

    /**
     * Send a plain-text message.
     *
     * @param toPhone recipient WhatsApp id (wa_id)
     * @param body    message text
     */
    void sendText(String toPhone, String body);
}
