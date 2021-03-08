package havis.util.cycle.common;

import havis.util.cycle.CycleData;
import havis.util.cycle.ImplementationException;
import havis.util.cycle.Initiation;
import havis.util.cycle.ReportFactory;
import havis.util.cycle.State;
import havis.util.cycle.Termination;
import havis.util.cycle.Trigger;
import havis.util.cycle.TriggerCallback;
import havis.util.cycle.TriggerFactory;
import havis.util.cycle.ValidationException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common cycle base class.
 * 
 * Start the cycle with {@link CommonCycle#start()}.
 * 
 * Implementations must call {@link CommonCycle#notify(String, Object)} when
 * data is collected.
 * 
 * @param <ConfigurationType>
 *            Type used in {@link CommonCycle#applyConfiguration(Object)} to
 *            configure the cycle
 * @param <NotifyType>
 *            Type for a single data set added to the cycle data
 * @param <CycleDataType>
 *            Type of cycle data, i.e. {@link SimpleCycleData}
 * @param <SubscriberType>
 *            Type of subscriber
 * @param <SubscriberListenerType>
 *            Type of subscriber listener
 */
public abstract class CommonCycle<ConfigurationType, NotifyType, CycleDataType extends CycleData<NotifyType>, SubscriberType, SubscriberListenerType>
		implements Runnable {

	private final static Logger log = Logger.getLogger(CommonCycle.class.getName());

	/**
	 * General lock for cycle state changes
	 */
	protected Lock lock = new ReentrantLock();

	/**
	 * Condition awaited before report generation, based on general lock
	 */
	protected Condition condition = lock.newCondition();

	/**
	 * Lock for subscribers
	 */
	protected Lock subscribersLock = new ReentrantLock();

	/**
	 * Lock for cycle data
	 */
	protected Lock dataLock = new ReentrantLock();

	/**
	 * The unique ID of the cycle
	 */
	protected String guid = UUID.randomUUID().toString();

	/**
	 * The current cycle state
	 */
	protected State state;

	/**
	 * The cycle duration, e.g. time span data is received from the data source
	 */
	protected long duration = -1;

	/**
	 * The cycle repeat period, e.g. time before the cycle repeats, disabled by
	 * default
	 */
	protected long repeatPeriod = -1;

	/**
	 * The duration which has to pass without any data changes before a report
	 * is generated, disabled by default
	 */
	protected long interval = -1;

	/**
	 * Whether to report data when it is available, disabled by default
	 */
	protected boolean whenDataAvailable = false;

	/**
	 * The delay before a report will be generated and sent if
	 * {@link CommonCycle#whenDataAvailable} is true and new data was received,
	 * 0 by default
	 */
	protected int whenDataAvailableDelay = 0;

	private Timer startTimer;
	private Timer intervalTimer;
	private Timer dataAvailableTimer;

	/**
	 * The cycle name
	 */
	protected String name;

	/**
	 * The cycle configuration
	 */
	protected ConfigurationType configuration;

	/**
	 * The factory for triggers, used by
	 * {@link CommonCycle#addStartTrigger(String)} and
	 * {@link CommonCycle#addStopTrigger(String)}
	 */
	protected TriggerFactory triggerFactory;

	/**
	 * The list of start and stop triggers, usually populated by
	 * {@link CommonCycle#addStartTrigger(String)} and
	 * {@link CommonCycle#addStopTrigger(String)}
	 */
	protected List<Trigger> triggers = new ArrayList<Trigger>();

	private boolean hasStartTriggers;
	private boolean hasStopTriggers;

	/**
	 * The current cycle data
	 */
	protected CycleDataType cycleData;

	/**
	 * The factory for reports
	 */
	protected ReportFactory<NotifyType, CycleDataType> reportFactory;

	/**
	 * The current cycle initiation reason
	 */
	protected Initiation initiation;

	/**
	 * The current initiation object, e.g. the URI of a start trigger
	 */
	protected Trigger initiator;

	/**
	 * The current cycle termination reason
	 */
	protected Termination termination;

	/**
	 * The current termination object, e.g. the URI of a stop trigger
	 */
	protected Trigger terminator;

	/**
	 * The thread the cycle {@link CommonCycle#run} method is called in
	 */
	protected Thread thread;

	private long lastCycleTriggeredTime = -1;

	private long nextCycleTriggeredTime = -1;

	private long lastCycleDurationDueTime = -1;

	private long nextCycleDurationDueTime = -1;

	/**
	 * Creates a new common cycle
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param configuration
	 *            the configuration of the cycle, will be passed to
	 *            {@link CommonCycle#applyConfiguration(Object)}
	 * @param cycleData
	 *            the cycle data instance to use
	 * @param reportFactory
	 *            the report factory to use for report generation
	 * @param triggerFactory
	 *            the trigger factory to use, can be null
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public CommonCycle(String name, ConfigurationType configuration, CycleDataType cycleData, ReportFactory<NotifyType, CycleDataType> reportFactory,
			TriggerFactory triggerFactory) throws ImplementationException, ValidationException {
		this.name = name;
		this.configuration = configuration;
		this.cycleData = Objects.requireNonNull(cycleData, "cycleData must not be null");
		this.reportFactory = Objects.requireNonNull(reportFactory, "reportFactory must not be null");
		this.triggerFactory = triggerFactory;
		applyConfiguration(configuration);
		validateConfiguration();
		this.state = State.UNREQUESTED;
		onStateChanged(name, this.state);
		this.thread = new Thread(this, this.getClass().getSimpleName() + " " + (this.name != null ? this.name : "[no name]"));
	}

	/**
	 * Creates a new common cycle
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param configuration
	 *            the configuration of the cycle, will be passed to
	 *            {@link CommonCycle#applyConfiguration(Object)}
	 * @param cycleData
	 *            the cycle data instance to use
	 * @param reportFactory
	 *            the report factory to use for report generation
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public CommonCycle(String name, ConfigurationType configuration, CycleDataType cycleData, ReportFactory<NotifyType, CycleDataType> reportFactory)
			throws ImplementationException, ValidationException {
		this(name, configuration, cycleData, reportFactory, null);
	}

	/**
	 * Apply the configuration, e.g. set duration, repeatPeriod, interval,
	 * whenDataAvailable, whenDataAvailableDelay and add start and stop triggers
	 * by calling either {@link CommonCycle#addStartTrigger(String)} or
	 * {@link CommonCycle#addStopTrigger(String)}
	 * 
	 * @param configuration
	 *            the configuration to apply
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected abstract void applyConfiguration(ConfigurationType configuration) throws ImplementationException, ValidationException;

	/**
	 * Validate the current configuration
	 * 
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected void validateConfiguration() throws ImplementationException, ValidationException {
		if (this.duration <= 0 && this.interval <= 0 && !this.hasStopTriggers && !whenDataAvailable)
			throw new ValidationException("No stop condition specifed");
	}

	/**
	 * Add a trigger to start the cycle. Usually this method is called from
	 * {@link CommonCycle#applyConfiguration(Object)}. The trigger will be
	 * created by the specified trigger factory and stored in the
	 * {@link CommonCycle}.
	 * 
	 * @param uri
	 *            the URI of the trigger
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected void addStartTrigger(String uri) throws ImplementationException, ValidationException {
		addTrigger(uri, true);
		hasStartTriggers = true;
	}

	/**
	 * Add a trigger to stop the cycle. Usually this method is called from
	 * {@link CommonCycle#applyConfiguration(Object)}. The trigger will be
	 * created by the specified trigger factory and stored in the
	 * {@link CommonCycle}.
	 * 
	 * @param uri
	 *            the URI of the trigger
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected void addStopTrigger(String uri) throws ImplementationException, ValidationException {
		addTrigger(uri, false);
		hasStopTriggers = true;
	}

	private void addTrigger(String uri, final boolean start) throws ImplementationException, ValidationException {
		if (this.triggerFactory == null)
			throw new ImplementationException("Failed to initialize trigger: TriggerFactory is not set");
		this.triggers.add(this.triggerFactory.create(this.guid, uri, new TriggerCallback() {
			@Override
			public boolean invoke(Trigger trigger) {
				if (start)
					return start(trigger);
				else
					return stop(trigger);
			}
		}));
	}

	/**
	 * Called when the cycle state changes
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param state
	 *            the target state
	 */
	protected void onStateChanged(String name, State state) {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Cycle ''{0}'' state changed to ''{1}''", new Object[] { name, state.toString() });
	}

	/**
	 * Called when start is triggered
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param trigger
	 *            the trigger, or null
	 */
	protected void onStartTriggered(String name, String trigger) {
		if (log.isLoggable(Level.INFO))
			log.log(Level.INFO, "Cycle ''{0}'' start was triggered by ''{1}''", new Object[] { name, trigger });
	}

	/**
	 * Called when stop is triggered
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param trigger
	 *            the trigger, or null
	 */
	protected void onStopTriggered(String name, String trigger) {
		if (log.isLoggable(Level.INFO))
			log.log(Level.INFO, "Cycle ''{0}'' stop was triggered by ''{1}''", new Object[] { name, trigger });
	}

	/**
	 * Called when the cycle starts
	 * 
	 * @param name
	 *            the name of the cycle
	 */
	protected void onCycleStarted(String name) {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Cycle ''{0}'' started", new Object[] { name });
	}

	/**
	 * Called when the cycle fails with an error
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param message
	 *            the error message
	 * @param error
	 *            the error
	 */
	protected void onCycleFailed(String name, String message, Exception error) {
		log.log(Level.SEVERE, message + ": " + name, error);
	}

	/**
	 * Called when the cycle is notified about new data
	 * 
	 * @param name
	 *            the name of the cycle
	 * @param source
	 *            the source of the notification, or null
	 * @param data
	 *            the data
	 */
	protected void onNotifyStarted(String name, String source, NotifyType data) {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Cycle ''{0}'' was notified by ''{1}''", new Object[] { name, source });
	}

	/**
	 * Called before a report is sent
	 * 
	 * @param name
	 *            the name of the cycle
	 */
	protected void onReportStarted(String name) {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Cycle ''{0}'' starts to send a report", new Object[] { name });
	}

	/**
	 * Called before the evaluation and report generation takes place
	 * 
	 * @param name
	 *            the name of the cycle
	 */
	protected void onEvaluateStarted(String name) {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Cycle ''{0}'' starts to evaluate", new Object[] { name });
	}

	/**
	 * Called when the cycle execution finishes
	 * 
	 * @param name
	 *            the name of the cycle
	 */
	protected void onCycleFinished(String name) {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Cycle ''{0}'' finished", new Object[] { name });
	}

	private void resetCycleTimes() {
		lastCycleTriggeredTime = -1;
		nextCycleTriggeredTime = -1;
		lastCycleDurationDueTime = -1;
		nextCycleDurationDueTime = -1;
	}

	/**
	 * Reschedule the interval timer, see {@link CommonCycle#interval}
	 * 
	 * @param delay
	 *            the delay
	 */
	protected void rescheduleIntervalTimer(long delay) {
		// TODO: don't use timer:
		// http://stackoverflow.com/questions/32001/resettable-java-timer
		if (this.intervalTimer != null) {
			this.intervalTimer.cancel();
		}
		this.intervalTimer = new Timer(this.getClass().getSimpleName() + ".intervalTimer " + (this.name != null ? this.name : "[no name]"));
		this.intervalTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				interruptByInterval();
			}
		}, delay);
	}

	private void rescheduleStartTimer(Date time) {
		if (this.startTimer != null) {
			this.startTimer.cancel();
		}
		this.startTimer = new Timer(this.getClass().getSimpleName() + ".startTimer " + (this.name != null ? this.name : "[no name]"));
		this.startTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				start(null);
			}
		}, time);
	}

	private boolean isDataAvailableTimerScheduled() {
		return this.dataAvailableTimer != null;
	}

	private void scheduleDataAvailableTimer() {
		int delay = Math.max(0, whenDataAvailableDelay);
		this.dataAvailableTimer = new Timer(this.getClass().getSimpleName() + ".dataAvailableTimer " + (this.name != null ? this.name : "[no name]"));
		this.dataAvailableTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				lock.lock();
				try {
					termination = Termination.DATA_AVAILABLE;
					condition.signal();
				} finally {
					lock.unlock();
				}
			}
		}, delay);
	}

	private void cancelDataAvailableTimer() {
		if (this.dataAvailableTimer != null) {
			this.dataAvailableTimer.cancel();
			dataAvailableTimer = null;
		}
	}

	/**
	 * Starts the cycle thread. The cycle is actually started, when it has at
	 * least one subscriber and either no start triggers are defined or one of
	 * the defined start triggers is invoked.
	 */
	public void start() {
		if (thread != null) {
			thread.start();
		}
	}

	/**
	 * @return the configuration
	 */
	public ConfigurationType getConfiguration() {
		return configuration;
	}

	/**
	 * Enables the data source, all data changes must be notified by calling
	 * {@link CommonCycle#notify(String, Object)}
	 * 
	 * @throws ImplementationException
	 *             if an internal error occurred, cycle thread will be stopped
	 */
	protected abstract void enable() throws ImplementationException;

	/**
	 * Disables the data source
	 * 
	 * @throws ImplementationException
	 *             if an internal error occurred, cycle thread will be stopped
	 */
	protected abstract void disable() throws ImplementationException;

	/**
	 * Starts the cycle
	 * 
	 * @param trigger
	 *            Optional trigger
	 */
	protected boolean start(Trigger trigger) {
		lock.lock();
		try {
			if (state == State.REQUESTED) {
				state = State.ACTIVE;
				if (nextCycleTriggeredTime == -1) {
					// not yet calculated, use current time
					lastCycleTriggeredTime = System.currentTimeMillis();
				} else {
					lastCycleTriggeredTime = nextCycleTriggeredTime;
				}
				onStateChanged(name, state);
				if (trigger instanceof Trigger) {
					initiation = Initiation.TRIGGER;
					onStartTriggered(name, trigger.getUri());
				}
				if (interval > 0) {
					// this makes sure that an old timer which didn't fire yet,
					// is reset
					rescheduleIntervalTimer(interval);
				}
				initiator = trigger;
				terminator = null; // reset
				condition.signal();
				return true;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	/**
	 * Interrupts the cycle by interval
	 */
	protected void interruptByInterval() {
		lock.lock();
		try {
			if (state == State.ACTIVE) {
				termination = Termination.INTERVAL;
				condition.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stops the cycle
	 * 
	 * @param trigger
	 *            Optional trigger instance if stop was called by a trigger
	 */
	protected boolean stop(Trigger trigger) {
		lock.lock();
		try {
			if (state == State.ACTIVE) {
				state = State.REQUESTED;
				resetCycleTimes();
				onStateChanged(name, state);
				if (trigger instanceof Trigger) {
					termination = Termination.TRIGGER;
					onStopTriggered(name, trigger.getUri());
				}
				terminator = trigger;
				condition.signal();
				return true;
			} else if (state == State.REQUESTED && initiation == Initiation.REPEAT_PERIOD) {
				// not active but repeat is scheduled
				if (startTimer != null) {
					startTimer.cancel();
				}
				resetCycleTimes();
				return true;
			}
		} finally {
			lock.unlock();
		}
		return false;
	}

	/**
	 * Send a report
	 * 
	 * @param report
	 *            the report to send
	 * @param listenersOnly
	 *            whether to send to listeners only
	 */
	protected abstract void sendReport(Object report, boolean listenersOnly);

	private void exec() {
		dataLock.lock();
		try {
			cycleData.prepare();
		} finally {
			dataLock.unlock();
		}

		onCycleStarted(name);

		if (interval > 0) {
			rescheduleIntervalTimer(interval);
		}

		long startTime = System.currentTimeMillis();

		try {
			termination = Termination.DURATION;
			if (duration <= 0) {
				condition.await(); // wait infinitely
			} else {
				lastCycleDurationDueTime = nextCycleDurationDueTime > -1 ? nextCycleDurationDueTime : startTime + duration;
				condition.awaitUntil(new Date(lastCycleDurationDueTime));
			}
		} catch (InterruptedException e) {
			lock.lock();
			onCycleFailed(name, "Cycle interrupted unacceptably [Duration]", e);
		}

		onEvaluateStarted(name);
		if (whenDataAvailable) {
			cancelDataAvailableTimer();
		}

		dataLock.lock();
		try {
			long creationTime = System.currentTimeMillis();
			long totalMilliseconds = creationTime - startTime;

			@SuppressWarnings("unchecked")
			CycleDataType clone = (CycleDataType) cycleData.clone();
			Object report = reportFactory.create(clone, new Date(creationTime), totalMilliseconds, initiation, initiator != null ? initiator.getUri() : null,
					termination, terminator != null ? terminator.getUri() : null);

			onReportStarted(name);

			sendReport(report, false);

			cycleData.rotate();

			nextCycleDurationDueTime = -1;

			// set cycle inactive if repeat period greater then duration
			// or cycle aborted i.e. by stop trigger
			if (state == State.ACTIVE) {
				if (repeatPeriod > -1) {
					repeatCycle();
				} else {
					state = State.REQUESTED;
					onStateChanged(name, state);
				}
			}
		} finally {
			dataLock.unlock();
		}
		updateState();
	}

	private void repeatCycle() {
		initiation = Initiation.REPEAT_PERIOD;
		if (repeatPeriod > duration) {
			nextCycleTriggeredTime = lastCycleTriggeredTime + repeatPeriod;
			if (nextCycleTriggeredTime > System.currentTimeMillis()) {
				state = State.REQUESTED;
				onStateChanged(name, state);
				rescheduleStartTimer(new Date(nextCycleTriggeredTime));
			} else {
				// next repeat is in the past
				// don't change the state,
				// therefore repeat immediately
				// and set current time
				lastCycleTriggeredTime = System.currentTimeMillis();
				nextCycleTriggeredTime = -1;

				if (duration > 0) {
					// compensate for any delays by specifying a
					// due time for the next duration
					nextCycleDurationDueTime = lastCycleDurationDueTime + duration;
				}
			}
		} else if (repeatPeriod == duration) {
			// compensate for any delays by specifying a
			// due time for the next duration
			nextCycleDurationDueTime = lastCycleDurationDueTime + duration;

			long timeToNextCycleEnd = nextCycleDurationDueTime - System.currentTimeMillis();
			if (timeToNextCycleEnd > repeatPeriod) {
				// we were signaled earlier than expected and
				// since we don't want to repeat earlier than
				// the repeat period, we schedule the start time
				state = State.REQUESTED;
				onStateChanged(name, state);

				long triggerTime = System.currentTimeMillis() + (timeToNextCycleEnd - repeatPeriod);
				long calculatedTriggerTime = lastCycleTriggeredTime + repeatPeriod;
				if (calculatedTriggerTime > System.currentTimeMillis()) {
					// same thing happened in the last cycle,
					// so we can use the pre calculated time
					triggerTime = calculatedTriggerTime;

					// also set the next time
					nextCycleTriggeredTime = calculatedTriggerTime;
				}

				rescheduleStartTimer(new Date(triggerTime));
			}
		} else if (duration > 0) {
			if (lastCycleDurationDueTime <= System.currentTimeMillis()) {
				// if we haven't been triggered earlier
				// compensate for any delays by specifying a
				// due time for the next duration
				nextCycleDurationDueTime = lastCycleDurationDueTime + duration;
			}
		}
	}

	@Override
	public void run() {
		lock.lock();
		try {
			while (State.UNREQUESTED.compareTo(state) <= 0) {
				while (State.REQUESTED.compareTo(state) <= 0) {
					if (State.ACTIVE.equals(state)) {
						enable();
						try {
							while (State.ACTIVE.equals(state)) {
								exec();
							}
						} finally {
							disable();
						}
					}
					if (State.REQUESTED.compareTo(state) <= 0) {
						condition.awaitUninterruptibly();
					} else {
						break;
					}
				}
				if (State.UNREQUESTED.compareTo(state) <= 0) {
					condition.awaitUninterruptibly();
				} else {
					break;
				}
			}
			// cycle already undefined
			if ((initiation == Initiation.UNDEFINE) && (termination == Termination.UNDEFINE)) {
				if (hasListeners()) {
					// send empty report to outstanding listeners
					Object report = reportFactory.create(null, new Date(), 0, initiation, null, termination, null);
					onReportStarted(name);
					sendReport(report, true);
				} else {
					reportFactory.dispose();
				}
			}
			condition.signal();
			onCycleFinished(name);
		} catch (Exception e) {
			thread = null;
			onCycleFailed(name, "Cycle aborted unexpectedly", e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @return true if enabled subscribers are present, false otherwise
	 */
	protected abstract boolean hasEnabledSubscribers();

	/**
	 * @return true if listeners are present, false otherwise
	 */
	protected abstract boolean hasListeners();

	/**
	 * Adds a subscriber. Starts the cycle if there are no start triggers.
	 * 
	 * @param subscriber
	 *            The subscriber
	 * 
	 * @return the subscriber id
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public String add(SubscriberType subscriber) throws ImplementationException, ValidationException {
		lock.lock();
		try {
			subscribersLock.lock();
			try {
				String id = addSubscriber(subscriber);
				evaluateCycleState();
				return id;
			} finally {
				subscribersLock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Add the subscriber
	 * 
	 * @param subscriber
	 *            the subscriber to add
	 * @return the ID of the subscriber
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected abstract String addSubscriber(SubscriberType subscriber) throws ImplementationException, ValidationException;

	/**
	 * Adds a subscriber listener. Starts the cycle if there are no start
	 * triggers.
	 * 
	 * @param listener
	 *            The listener
	 * 
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public void addListener(SubscriberListenerType listener) throws ImplementationException, ValidationException {
		lock.lock();
		try {
			subscribersLock.lock();
			try {
				addSubscriberListener(listener);
				evaluateCycleState();
			} finally {
				subscribersLock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	protected abstract void addSubscriberListener(SubscriberListenerType listener) throws ImplementationException, ValidationException;

	/**
	 * Evaluate the cycle state when the subscribers changed without calling
	 * {@link CommonCycle#add(Object)}, {@link CommonCycle#update(Object)} or
	 * {@link CommonCycle#remove(Object)} directly.
	 */
	public void evaluateCycleState() {
		if (!State.UNDEFINED.equals(state)) {
			if (hasEnabledSubscribers() && State.UNREQUESTED.compareTo(state) >= 0) {
				lock.lock();
				try {
					if (hasEnabledSubscribers() && State.UNREQUESTED.compareTo(state) >= 0) {
						state = State.REQUESTED;
						onStateChanged(name, state);
						if (!hasStartTriggers) {
							initiation = Initiation.REQUESTED;
							start((Trigger) null);
						}
					}
				} finally {
					lock.unlock();
				}
			}
			if (!hasEnabledSubscribers() && State.UNREQUESTED.compareTo(state) < 0) {
				lock.lock();
				try {
					if (!hasEnabledSubscribers() && State.UNREQUESTED.compareTo(state) < 0) {
						if (State.ACTIVE.equals(state)) {
							termination = Termination.UNREQUESTED;
						}
						state = State.UNREQUESTED;
						cycleData.reset();
						resetCycleTimes();
						onStateChanged(name, state);
						condition.signal();
					}
				} finally {
					lock.unlock();
				}
			}
		}
	}

	/**
	 * Removes a subscriber
	 * 
	 * @param subscriber
	 *            The subscriber
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public void remove(SubscriberType subscriber) throws ImplementationException, ValidationException {
		lock.lock();
		try {
			subscribersLock.lock();
			try {
				removeSubscriber(subscriber);
				evaluateCycleState();
			} finally {
				subscribersLock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Remove a subscriber
	 * 
	 * @param subscriber
	 *            the subscriber
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected abstract void removeSubscriber(SubscriberType subscriber) throws ImplementationException, ValidationException;

	/**
	 * Updates a subscriber
	 * 
	 * @param subscriber
	 *            The subscriber
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public void update(SubscriberType subscriber) throws ImplementationException, ValidationException {
		lock.lock();
		try {
			subscribersLock.lock();
			try {
				updateSubscriber(subscriber);
				evaluateCycleState();
			} finally {
				subscribersLock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Remove a subscriber
	 * 
	 * @param subscriber
	 *            the subscriber
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	protected abstract void updateSubscriber(SubscriberType subscriber) throws ImplementationException, ValidationException;

	private void updateState() {
		subscribersLock.lock();
		try {
			if (!hasEnabledSubscribers() && State.REQUESTED.compareTo(state) <= 0) {
				state = State.UNREQUESTED;
				cycleData.reset();
				resetCycleTimes();
				onStateChanged(name, state);
			}
		} finally {
			subscribersLock.unlock();
		}
	}

	/**
	 * Returns if the cycle is busy
	 * 
	 * @return true if the cycle is busy, false otherwise
	 */
	public boolean isBusy() {
		return State.UNREQUESTED.compareTo(state) < 0; // state > UNREQUESTED
	}

	/**
	 * Returns if the cycle is active
	 * 
	 * @return true if the cycle is active, false otherwise
	 */
	protected boolean isActive() {
		return state == State.ACTIVE;
	}

	/**
	 * Notifies the cycle about new data
	 * 
	 * @param source
	 *            The name of the source, or null
	 * @param data
	 *            The data
	 */
	protected void notify(String source, NotifyType data) {
		while (isActive()) {
			try {
				if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
					try {
						if (isActive()) {
							dataLock.lock();
							try {
								onNotifyStarted(name, source, data);
								if (this.cycleData.add(data)) {
									if (whenDataAvailable && !isDataAvailableTimerScheduled()) {
										scheduleDataAvailableTimer();
									} else if (interval > 0) {
										rescheduleIntervalTimer(interval);
									}
								}
							} finally {
								dataLock.unlock();
							}
						}
						break;
					} finally {
						lock.unlock();
					}
				}
			} catch (InterruptedException e) {
				break; // ignore data
			}
		}
	}

	/**
	 * Disposes the cycle and all dependent data
	 */
	public void dispose() {
		boolean locked = false;
		try {
			locked = lock.tryLock(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}

		// stop waiting for any missing data
		if (cycleData != null) {
			cycleData.dispose();
		}

		if (!locked) {
			lock.lock();
		}

		try {
			if (intervalTimer != null) {
				intervalTimer.cancel();
			}
			if (startTimer != null) {
				startTimer.cancel();
			}
			termination = Termination.UNDEFINE;
			if (State.UNDEFINED.compareTo(state) < 0 /* state > UNDEFINED */) {
				switch (state) {
				case UNREQUESTED:
					reportFactory.dispose();
					break;
				case REQUESTED:
					initiation = Initiation.UNDEFINE;
					break;
				default:
					break;
				}
				state = State.UNDEFINED;
				onStateChanged(name, state);
				if (thread != null && thread.isAlive()) {
					condition.signal();
					condition.awaitUninterruptibly();
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				disposeSubscribers();
			}
		} finally {
			lock.unlock();
		}

		for (Trigger trigger : this.triggers) {
			trigger.dispose();
		}
		this.triggers.clear();
		disposeCycle();
	}

	/**
	 * Dispose subscribers
	 */
	protected abstract void disposeSubscribers();

	/**
	 * Dispose cycle internals, usually there's nothing to do
	 */
	protected abstract void disposeCycle();
}
