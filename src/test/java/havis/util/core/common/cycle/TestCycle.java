package havis.util.core.common.cycle;

import havis.transport.Subscriber;
import havis.transport.SubscriberListener;
import havis.transport.SubscriberManager;
import havis.util.cycle.ImplementationException;
import havis.util.cycle.Initiation;
import havis.util.cycle.ReportFactory;
import havis.util.cycle.State;
import havis.util.cycle.Termination;
import havis.util.cycle.TriggerFactory;
import havis.util.cycle.ValidationException;
import havis.util.cycle.common.CommonCycle;
import havis.util.cycle.common.ListCycleData;

import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestCycle extends CommonCycle<TestConf, SimpleEntry<String, Object>, ListCycleData<SimpleEntry<String, Object>>, Subscriber, SubscriberListener> {

	private SubscriberManager manager;
	private Thread thread;
	private CountDownLatch latch;

	public TestCycle(String name, TestConf configuration, SubscriberManager manager, TriggerFactory triggerFactory) throws ImplementationException,
			ValidationException {
		super(name, configuration, new ListCycleData<SimpleEntry<String, Object>>(), new ReportFactory<SimpleEntry<String, Object>, ListCycleData<SimpleEntry<String, Object>>>() {
			@Override
			public Object create(ListCycleData<SimpleEntry<String, Object>> data, Date date, long totalMilliseconds, Initiation initiation, String initiator,
					Termination termination, String terminator) {
				data.getData().add(new SimpleEntry<String, Object>("totalMilliseconds", totalMilliseconds));
				data.getData().add(new SimpleEntry<String, Object>("initiation", initiation));
				data.getData().add(new SimpleEntry<String, Object>("initiator", initiator));
				data.getData().add(new SimpleEntry<String, Object>("termination", termination));
				data.getData().add(new SimpleEntry<String, Object>("terminator", terminator));
				return new TestReport(data.getData().toString());
			}

			@Override
			public void dispose() {
			}
		}, triggerFactory);
		this.manager = manager;
	}

	@Override
	protected void onStateChanged(String name, State state) {
		super.onStateChanged(name, state);
		System.out.println("State changed to " + state);
	}

	@Override
	protected void onCycleStarted(String name) {
		super.onCycleStarted(name);
		System.out.println("Cycle started");
	}

	@Override
	protected void onCycleFinished(String name) {
		super.onCycleFinished(name);
		System.out.println("Cycle finished");
	}

	@Override
	protected void onEvaluateStarted(String name) {
		super.onEvaluateStarted(name);
		System.out.println("Cycle starts evaluation");
	}

	@Override
	protected void applyConfiguration(TestConf configuration) throws ImplementationException, ValidationException {
		this.duration = 1000;
		this.repeatPeriod = 0;
	}

	@Override
	protected void enable() {
		if (thread == null) {
			latch = new CountDownLatch(1);
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						int count = 0;
						while (true) {
							TestCycle.this.notify("source", new SimpleEntry<String, Object>(Integer.toString(count++), "data"));
							if (latch.await(100, TimeUnit.MILLISECONDS)) {
								return;
							}
						}
					} catch (InterruptedException e) {
						return;
					}
				}
			});
			thread.start();
		}
	}

	@Override
	protected void disable() {
		if (thread != null) {
			latch.countDown();
			thread = null;
		}
	}

	@Override
	protected void sendReport(Object report, boolean listenersOnly) {
		manager.send(report, listenersOnly);
	}

	@Override
	protected boolean hasEnabledSubscribers() {
		return manager.hasEnabledSubscribers();
	}

	@Override
	protected boolean hasListeners() {
		return manager.hasListeners();
	}

	@Override
	protected String addSubscriber(Subscriber subscriber) throws ImplementationException, ValidationException {
		try {
			return manager.add(subscriber);
		} catch (havis.transport.ValidationException e) {
			throw new ValidationException(e.getMessage());
		}
	}

	@Override
	protected void addSubscriberListener(SubscriberListener listener) throws ImplementationException, ValidationException {
		try {
			manager.add(listener);
		} catch (havis.transport.ValidationException e) {
			throw new ValidationException(e.getMessage());
		}
	}

	@Override
	protected void removeSubscriber(Subscriber subscriber) throws ImplementationException, ValidationException {
		try {
			manager.remove(subscriber);
		} catch (havis.transport.ValidationException e) {
			throw new ValidationException(e.getMessage());
		}
	}

	@Override
	protected void updateSubscriber(Subscriber subscriber) throws ImplementationException, ValidationException {
		try {
			manager.update(subscriber);
		} catch (havis.transport.ValidationException e) {
			throw new ValidationException(e.getMessage());
		}
	}

	@Override
	protected void disposeSubscribers() {
		manager.dispose(false);
	}

	@Override
	protected void disposeCycle() {
	}
}
