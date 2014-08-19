package org.github.alexwibowo.mockifix

import org.apache.mina.common.IoSession
import org.apache.mina.common.WriteFuture
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quickfix.MessageUtils
import quickfix.SessionID
import quickfix.field.MsgType

import java.util.concurrent.atomic.AtomicInteger


/**
 * User: alexwibowo
 */
class MockifixMessageProcessor {

    public static final Logger LOGGER = LoggerFactory.getLogger(MockifixMessageProcessor.class.name)
    List<String> receivedMessages = Collections.synchronizedList([])

    private final AtomicInteger sequence = new AtomicInteger(1)

    private final boolean synchronousWrites = true
    private final long synchronousWriteTimeout = 2000

    IoSession ioSession

    void processMessage(String message){
        SessionID sessionID = MessageUtils.getSessionID(message)
        String messageType = MessageUtils.getMessageType(message)
        if (messageType == MsgType.LOGON) {
            processLogonMessage(message)
        }
        receivedMessages <<  message
    }

    void processLogonMessage(String message) {
        SessionID reverseSession = MessageUtils.getReverseSessionID(message)
        String fixVersion = MessageUtils.getStringField(message, 8)
        String sequenceNumber = MessageUtils.getStringField(message, 34)
        String heartbeatInterval = MessageUtils.getStringField(message, 108)
        String encryptMethod = MessageUtils.getStringField(message, 98)
        def response = new DecoratedFixMessage("8=${fixVersion}\00135=A\00134=${sequence.getAndIncrement()}\00149=${reverseSession.senderCompID}\00152=<TIME>\00156=${reverseSession.targetCompID}\00198=${encryptMethod}\001108=${heartbeatInterval}\001").toString()
        sendMessage(response)
    }

    void sendMessage(String message) {
        WriteFuture future =  ioSession.write(message)
        if (synchronousWrites) {
            try {
                if (!future.join(synchronousWriteTimeout)) {
                    LOGGER.error("Synchronous write timed out after " + synchronousWriteTimeout + "ms");
                }
            } catch (RuntimeException e) {
                LOGGER.error("Synchronous write failed: " + e.getMessage());
            }
        }
    }
}