package com.logimethods.camel;

import junit.framework.TestCase;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SpringCamelTest extends TestCase {

    @Autowired
    private ProducerTemplate producer;

    @Autowired
    private ConsumerTemplate consumer;

    @EndpointInject(ref = "result")
    private MockEndpoint mock;

    @Test
    public void testConsumeTemplate() throws Exception {
        // we expect Hello World received in our mock endpoint
        mock.expectedBodiesReceived("Hello World");

        // we use the producer template to send a message to the seda:start endpoint
        producer.sendBody("seda:start", "Hello World");

        // we consume the body from seda:start
        String body = consumer.receiveBody("seda:start", String.class);
        assertEquals("Hello World", body);

        // and then we send the body again to seda:foo so it will be routed to the mock
        // endpoint so our unit test can complete
        producer.sendBody("seda:foo", body);

        // assert mock received the body
        mock.assertIsSatisfied();
    }

}
