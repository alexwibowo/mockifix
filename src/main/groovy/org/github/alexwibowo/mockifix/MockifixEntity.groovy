package org.github.alexwibowo.mockifix

import org.apache.mina.common.IoAcceptor
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.transport.socket.nio.SocketAcceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quickfix.mina.message.FIXProtocolCodecFactory

import java.util.concurrent.CountDownLatch

/**
 * User: alexwibowo
 */
class MockifixEntity implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockifixEntity.class.getName())

    String entityId

    private final SocketAddress socketAddress

    IoAcceptor acceptor

    MockifixMessageProcessor messageProcessor

    private final CountDownLatch initializationLatch = new CountDownLatch(1)
    private final CountDownLatch shutdownLatch = new CountDownLatch(1)

    MockifixEntity(int port) {
        acceptor = new SocketAcceptor()
        socketAddress = new InetSocketAddress(port)

        // log all information such as newly created sessions, messages received, messages sent, session closed.
        // translate binary or protocol specific data into message object and vice versa
        // We use an existing TextLine factory because it will handle text base message for you
        def protocolCodecFilter = new ProtocolCodecFilter(new FIXProtocolCodecFactory())
        acceptor.filterChain.addLast("FIXCodec", protocolCodecFilter)
    }

    @Override
    void run() {
        acceptor.bind(socketAddress, new MockifixIoHandler(messageProcessor: messageProcessor))

        initializationLatch.countDown()
        LOGGER.info("Mockifix ${entityId} is fully initialized.");
        try {
            shutdownLatch.await()
            LOGGER.info("Mockifix ${entityId} is stopped.")
        } catch (InterruptedException e1) {
            try {
                LOGGER.info("Failed to shutdown. Stopping acceptor..")
                acceptor.unbind(socketAddress)
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

    }

    void stop() {
        LOGGER.info("Stopping down Mockifix ${entityId}.")
        acceptor.unbind(socketAddress)
        shutdownLatch.countDown()
    }
}
