package org.github.alexwibowo.mockifix

import quickfix.SessionSettings
import quickfix.field.MDReqID
import quickfix.field.MarketDepth
import quickfix.field.SubscriptionRequestType
import quickfix.fix44.MarketDataRequest
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * User: alexwibowo
 */


class TestChat extends Specification{

    @Shared
    private ExecutorService executor = Executors.newSingleThreadExecutor()

    MockifixMessageProcessor processor

    MockifixEntity entity

    def setup() {
        processor = new MockifixMessageProcessor()
        entity = new MockifixEntity(9880)
        entity.messageProcessor = processor
    }


    def "should be able to start Mockifix and make it listen for incoming connection"() {
        given:   "mockifix is listening"
        executor.execute(entity) // make mockifix starts listening. TODO: i dont like this . prefer for it to say "listen()"

        and:   "a client is set up to connect to mockifix"
        InputStream inputStream = InitiatingFixExecutor.class.getResourceAsStream("executor.cfg")
        SessionSettings settings = new SessionSettings(inputStream);
        inputStream.close();
        InitiatingFixExecutor executor = new InitiatingFixExecutor(settings);

        and: "the client is started up"
        executor.start();

        when: "the client is sending an MDR message"
          // TODO: this is stupid. This is just so that the login is initiated before sending any MDR.
        // if we dont sleep, then the test will send the MDR before the login... and well.. mockifix wont see the MDR.
        Thread.sleep(10000)
        def mdr = new MarketDataRequest(
                new MDReqID("MDR-ID"),
                new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT),
                new MarketDepth()
        )
        executor.sendMessage(mdr)

        then:
        Thread.sleep(10000)
        println processor.receivedMessages
    }
}
