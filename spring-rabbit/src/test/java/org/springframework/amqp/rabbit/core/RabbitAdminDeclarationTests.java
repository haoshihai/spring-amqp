/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.amqp.rabbit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 1.2
 *
 */
public class RabbitAdminDeclarationTests {

	@Test
	public void testUnconditional() throws Exception {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection conn = mock(Connection.class);
		Channel channel = mock(Channel.class);
		when(cf.createConnection()).thenReturn(conn);
		when(conn.createChannel(false)).thenReturn(channel);
		when(channel.queueDeclare("foo", true, false, false, null)).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));
		final AtomicReference<ConnectionListener> listener = new AtomicReference<ConnectionListener>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				listener.set((ConnectionListener) invocation.getArguments()[0]);
				return null;
			}
		}).when(cf).addConnectionListener(any(ConnectionListener.class));
		RabbitAdmin admin = new RabbitAdmin(cf);
		GenericApplicationContext context = new GenericApplicationContext();
		Queue queue = new Queue("foo");
		context.getBeanFactory().registerSingleton("foo", queue);
		DirectExchange exchange = new DirectExchange("bar");
		context.getBeanFactory().registerSingleton("bar", exchange);
		Binding binding = new Binding("foo", DestinationType.QUEUE, "bar", "foo", null);
		context.getBeanFactory().registerSingleton("baz", binding);
		context.refresh();
		admin.setApplicationContext(context);
		admin.afterPropertiesSet();
		assertNotNull(listener.get());
		listener.get().onCreate(conn);

		verify(channel).queueDeclare("foo", true, false, false, null);
		verify(channel).exchangeDeclare("bar", "direct", true, false, new HashMap<String, Object>());
		verify(channel).queueBind("foo", "bar", "foo", null);
	}

	@Test
	public void testUnconditionalWithExplicitFactory() throws Exception {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection conn = mock(Connection.class);
		Channel channel = mock(Channel.class);
		when(cf.createConnection()).thenReturn(conn);
		when(conn.createChannel(false)).thenReturn(channel);
		when(channel.queueDeclare("foo", true, false, false, null)).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));
		final AtomicReference<ConnectionListener> listener = new AtomicReference<ConnectionListener>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				listener.set((ConnectionListener) invocation.getArguments()[0]);
				return null;
			}
		}).when(cf).addConnectionListener(any(ConnectionListener.class));
		RabbitAdmin admin = new RabbitAdmin(cf);
		GenericApplicationContext context = new GenericApplicationContext();
		Queue queue = new Queue("foo");
		queue.setAdminsThatShouldDeclare(admin);
		context.getBeanFactory().registerSingleton("foo", queue);
		DirectExchange exchange = new DirectExchange("bar");
		exchange.setAdminsThatShouldDeclare(admin);
		context.getBeanFactory().registerSingleton("bar", exchange);
		Binding binding = new Binding("foo", DestinationType.QUEUE, "bar", "foo", null);
		binding.setAdminsThatShouldDeclare(admin);
		context.getBeanFactory().registerSingleton("baz", binding);
		context.refresh();
		admin.setApplicationContext(context);
		admin.afterPropertiesSet();
		assertNotNull(listener.get());
		listener.get().onCreate(conn);

		verify(channel).queueDeclare("foo", true, false, false, null);
		verify(channel).exchangeDeclare("bar", "direct", true, false, new HashMap<String, Object>());
		verify(channel).queueBind("foo", "bar", "foo", null);
	}

	@Test
	public void testSkipBecauseDifferentFactory() throws Exception {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection conn = mock(Connection.class);
		Channel channel = mock(Channel.class);
		when(cf.createConnection()).thenReturn(conn);
		when(conn.createChannel(false)).thenReturn(channel);
		when(channel.queueDeclare("foo", true, false, false, null)).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));
		final AtomicReference<ConnectionListener> listener = new AtomicReference<ConnectionListener>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				listener.set((ConnectionListener) invocation.getArguments()[0]);
				return null;
			}
		}).when(cf).addConnectionListener(any(ConnectionListener.class));
		RabbitAdmin admin = new RabbitAdmin(cf);
		RabbitAdmin other = new RabbitAdmin(cf);
		GenericApplicationContext context = new GenericApplicationContext();
		Queue queue = new Queue("foo");
		queue.setAdminsThatShouldDeclare(other);
		context.getBeanFactory().registerSingleton("foo", queue);
		DirectExchange exchange = new DirectExchange("bar");
		exchange.setAdminsThatShouldDeclare(other);
		context.getBeanFactory().registerSingleton("bar", exchange);
		Binding binding = new Binding("foo", DestinationType.QUEUE, "bar", "foo", null);
		binding.setAdminsThatShouldDeclare(other);
		context.getBeanFactory().registerSingleton("baz", binding);
		context.refresh();
		admin.setApplicationContext(context);
		admin.afterPropertiesSet();
		assertNotNull(listener.get());
		listener.get().onCreate(conn);

		verify(channel, times(0)).queueDeclare("foo", true, false, false, null);
		verify(channel, times(0)).exchangeDeclare("bar", "direct", true, false, new HashMap<String, Object>());
		verify(channel, times(0)).queueBind("foo", "bar", "foo", null);
	}

	@Test
	public void testSkipBecauseShouldntDeclare() throws Exception {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection conn = mock(Connection.class);
		Channel channel = mock(Channel.class);
		when(cf.createConnection()).thenReturn(conn);
		when(conn.createChannel(false)).thenReturn(channel);
		when(channel.queueDeclare("foo", true, false, false, null)).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));
		final AtomicReference<ConnectionListener> listener = new AtomicReference<ConnectionListener>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				listener.set((ConnectionListener) invocation.getArguments()[0]);
				return null;
			}
		}).when(cf).addConnectionListener(any(ConnectionListener.class));
		RabbitAdmin admin = new RabbitAdmin(cf);
		GenericApplicationContext context = new GenericApplicationContext();
		Queue queue = new Queue("foo");
		queue.setShouldDeclare(false);
		context.getBeanFactory().registerSingleton("foo", queue);
		DirectExchange exchange = new DirectExchange("bar");
		exchange.setShouldDeclare(false);
		context.getBeanFactory().registerSingleton("bar", exchange);
		Binding binding = new Binding("foo", DestinationType.QUEUE, "bar", "foo", null);
		binding.setShouldDeclare(false);
		context.getBeanFactory().registerSingleton("baz", binding);
		context.refresh();
		admin.setApplicationContext(context);
		admin.afterPropertiesSet();
		assertNotNull(listener.get());
		listener.get().onCreate(conn);

		verify(channel, times(0)).queueDeclare("foo", true, false, false, null);
		verify(channel, times(0)).exchangeDeclare("bar", "direct", true, false, new HashMap<String, Object>());
		verify(channel, times(0)).queueBind("foo", "bar", "foo", null);
	}

	@Test
	public void testJavaConfig() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		Config.listener1.onCreate(Config.conn1);
		verify(Config.channel1).queueDeclare("foo", true, false, false, null);
		verify(Config.channel1).exchangeDeclare("bar", "direct", true, false, new HashMap<String, Object>());
		verify(Config.channel1).queueBind("foo", "bar", "foo", null);

		Config.listener2.onCreate(Config.conn2);
		verify(Config.channel2, times(0)).queueDeclare("foo", true, false, false, null);
		verify(Config.channel2, times(0)).exchangeDeclare("bar", "direct", true, false, new HashMap<String, Object>());
		verify(Config.channel2, times(0)).queueBind("foo", "bar", "foo", null);
		context.destroy();
	}

	@Test
	public void testAddRemove() {
		Queue queue = new Queue("foo");
		ConnectionFactory cf = mock(ConnectionFactory.class);
		RabbitAdmin admin1 = new RabbitAdmin(cf);
		RabbitAdmin admin2 = new RabbitAdmin(cf);
		queue.setAdminsThatShouldDeclare(admin1, admin2);
		assertEquals(2, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare(admin1);
		assertEquals(1, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare(new AmqpAdmin[] {null});
		assertEquals(0, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare(admin1, admin2);
		assertEquals(2, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare();
		assertEquals(0, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare(admin1, admin2);
		assertEquals(2, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare((AmqpAdmin) null);
		assertEquals(0, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare(admin1, admin2);
		assertEquals(2, queue.getDeclaringAdmins().size());
		queue.setAdminsThatShouldDeclare((AmqpAdmin[]) null);
		assertEquals(0, queue.getDeclaringAdmins().size());
		try {
			queue.setAdminsThatShouldDeclare(null, admin1);
			fail("Expected Exception");
		}
		catch (IllegalArgumentException e) {}
	}

	@Configuration
	public static class Config {

		private static Connection conn1 = mock(Connection.class);

		private static Connection conn2 = mock(Connection.class);

		private static Channel channel1 = mock(Channel.class);

		private static Channel channel2 = mock(Channel.class);

		private static ConnectionListener listener1;

		private static ConnectionListener listener2;

		@Bean
		public ConnectionFactory cf1() throws IOException {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			when(connectionFactory.createConnection()).thenReturn(conn1);
			when(conn1.createChannel(false)).thenReturn(channel1);
			when(channel1.queueDeclare("foo", true, false, false, null)).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));
			doAnswer(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					listener1 = (ConnectionListener) invocation.getArguments()[0];
					return null;
				}
			}).when(connectionFactory).addConnectionListener(any(ConnectionListener.class));
			return connectionFactory;
		}

		@Bean
		public ConnectionFactory cf2() throws IOException {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			when(connectionFactory.createConnection()).thenReturn(conn2);
			when(conn2.createChannel(false)).thenReturn(channel2);
			when(channel2.queueDeclare("foo", true, false, false, null)).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));
			doAnswer(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					listener2 = (ConnectionListener) invocation.getArguments()[0];
					return null;
				}
			}).when(connectionFactory).addConnectionListener(any(ConnectionListener.class));
			return connectionFactory;
		}

		@Bean
		public RabbitAdmin admin1() throws IOException {
			RabbitAdmin rabbitAdmin = new RabbitAdmin(cf1());
			rabbitAdmin.afterPropertiesSet();
			return rabbitAdmin;
		}

		@Bean
		public RabbitAdmin admin2() throws IOException {
			RabbitAdmin rabbitAdmin = new RabbitAdmin(cf2());
			rabbitAdmin.afterPropertiesSet();
			return rabbitAdmin;
		}

		@Bean
		public Queue queue() throws IOException {
			Queue queue = new Queue("foo");
			queue.setAdminsThatShouldDeclare(admin1());
			return queue;
		}

		@Bean
		public Exchange exchange() throws IOException {
			DirectExchange exchange = new DirectExchange("bar");
			exchange.setAdminsThatShouldDeclare(admin1());
			return exchange;
		}

		@Bean
		public Binding binding() throws IOException {
			Binding binding = new Binding("foo", DestinationType.QUEUE, exchange().getName(), "foo", null);
			binding.setAdminsThatShouldDeclare(admin1());
			return binding;
		}
	}
}
