package org.github.alexwibowo.mockifix

import org.apache.mina.common.IdleStatus
import org.apache.mina.common.IoHandlerAdapter
import org.apache.mina.common.IoSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * User: alexwibowo
 */
class MockifixIoHandler extends IoHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockifixIoHandler.class.getName())

    MockifixMessageProcessor messageProcessor

    @Override
    void sessionCreated(IoSession session) throws Exception {
        LOGGER.info("MINA session created: local=${session.localAddress},${session.getClass()}, remote=${session.remoteAddress}")
        super.sessionCreated(session)
        messageProcessor.ioSession = session
    }


    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        LOGGER.error("An exception has been caught for session: local=${session.localAddress}, remote= ${session.remoteAddress}", cause)
        cause.printStackTrace();
    }

    @Override
    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception {
        System.out.println( "IDLE " + session.getIdleCount( status ));
    }

    @Override
    void messageReceived(IoSession session, Object message) throws Exception {
        super.messageReceived(session, message)
        LOGGER.info("Message received ${message}");
        String messageString = (String) message;
        messageProcessor.processMessage(messageString);
    }
}
