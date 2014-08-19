package org.github.alexwibowo.mockifix

import org.quickfixj.jmx.JmxExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * User: alexwibowo
 */
import quickfix.*

import javax.management.JMException
import javax.management.ObjectName

public class InitiatingFixExecutor {
    private final static Logger log = LoggerFactory.getLogger(InitiatingFixExecutor.class);
    private final SocketInitiator initiator;
    private final JmxExporter jmxExporter;
    private final ObjectName connectorObjectName;

    public InitiatingFixExecutor(SessionSettings settings) throws ConfigError, FieldConvertError, JMException {
        Application application = new Application();
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();

        initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory,
                messageFactory);

        jmxExporter = new JmxExporter();
        connectorObjectName = jmxExporter.register(initiator);
        log.info("Acceptor registered with JMX, name=" + connectorObjectName);
    }

    public void sendMessage(Message message) throws SessionNotFound {
        SessionID sessionID = initiator.getSessions().get(0);
        Session.sendToTarget(message, sessionID);
    }

    public void start() throws RuntimeError, ConfigError {
        initiator.start();
    }

    private void stop() {
        try {
            jmxExporter.getMBeanServer().unregisterMBean(connectorObjectName);
        } catch (Exception e) {
            log.error("Failed to unregister acceptor from JMX", e);
        }
        initiator.stop();
    }
}
