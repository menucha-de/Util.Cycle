package havis.util.core.common.cycle;

import havis.transform.common.JsTransformerFactory;
import havis.transport.MessageReceiver;
import havis.transport.Messenger;
import havis.transport.Subscriber;
import havis.transport.SubscriberListener;
import havis.transport.SubscriberManager;
import havis.transport.common.CommonSubscriberManager;
import havis.transport.common.Connector;
import havis.transport.common.Provider;
import havis.util.cycle.common.CommonMessageReceiver;
import havis.util.monitor.Broker;
import havis.util.monitor.Event;
import havis.util.monitor.Source;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class TestConf {
	public static void main(String[] args) throws Exception {
		final Providers providers = (Providers) RuntimeDelegate.getInstance();
		RegisterBuiltin.register((ResteasyProviderFactory) providers);
		Provider.createFactory(new Provider() {
			@Override
			public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
				ResteasyProviderFactory.pushContext(Providers.class, providers);
				return providers.getMessageBodyWriter(clazz, type, annotations, mediaType);
			}

			@Override
			public <T> void write(MessageBodyWriter<T> writer, T data, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType,
					MultivaluedMap<String, Object> properties, OutputStream stream) throws Exception {
				ResteasyProviderFactory.pushContext(Providers.class, providers);
				writer.writeTo(data, clazz, type, annotations, mediaType, properties, stream);
			}

			@Override
			public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
				return null;
			}

			@Override
			public <T> T read(MessageBodyReader<T> arg0, Class<T> arg1, Type arg2, Annotation[] arg3, MediaType arg4, MultivaluedMap<String, String> arg5,
					InputStream arg6) throws Exception {
				return null;
			}
		});
		Connector.createFactory(new Connector() {
			@SuppressWarnings("unchecked")
			@Override
			public <S> S newInstance(Class<S> clazz, String type) throws havis.transport.ValidationException {
				if ("javascript".equals(type)) {
					return (S) new JsTransformerFactory().newInstance();
				}
				return null;
			}

			@Override
			public <S> List<String> getTypes(Class<S> clazz) throws havis.transport.ValidationException {
				return null;
			}

			@Override
			public Broker getBroker() {
				return new Broker() {
					@Override
					public void notify(Source arg0, Event arg1) {
					}
				};
			}
		});

		Map<String, String> defaultProperties = new HashMap<>();
		defaultProperties.put(Messenger.MIMETYPE_PROPERTY, "application/json");
		SubscriberManager subscribers = new CommonSubscriberManager(TestReport.class, new ArrayList<Subscriber>(), defaultProperties);
		subscribers.add(new Subscriber(true, "tcp://localhost:5555"));

		TestTriggerFactory triggerFactory = new TestTriggerFactory();
		TestCycle cycle = new TestCycle("test", new TestConf(), subscribers, triggerFactory);

		cycle.start();
		cycle.evaluateCycleState();

		Thread.sleep(1000);
		cycle.addListener(new SubscriberListener(new MessageReceiver() {
			@Override
			public void receive(Object message) {
				System.out.println(message);
			}

			@Override
			public void cancel() {
				System.out.println("CANCEL");
			}
		}));
		Thread.sleep(5000);
		cycle.addListener(new SubscriberListener(new MessageReceiver() {
			@Override
			public void receive(Object message) {
				System.out.println(message);
			}

			@Override
			public void cancel() {
				System.out.println("CANCEL");
			}
		}));
		Thread.sleep(5000);
		CommonMessageReceiver<TestReport> r = new CommonMessageReceiver<TestReport>();
		cycle.addListener(new SubscriberListener(r));
		System.out.println(r.get());
		cycle.dispose();
	}
}
