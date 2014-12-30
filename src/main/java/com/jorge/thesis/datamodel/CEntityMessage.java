package com.jorge.thesis.datamodel;

public class CEntityMessage {

    private final String messageId;

    private CEntityMessage(String _messageId) {
        messageId = _messageId;
    }

    String getMessageId() {
        return messageId;
    }

    /**
     * The purpose of this class is to take out from the programmer the responsibility of finding an id for the message.
     */
    public class Builder {

        public synchronized CEntityMessage build() {
            return new CEntityMessage(MessageManagerSingleton.getInstance().generateMessageId());
        }
    }
}
