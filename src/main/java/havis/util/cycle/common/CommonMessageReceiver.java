package havis.util.cycle.common;

import havis.transport.MessageReceiver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Message receiver to wait for a single message (not reusable). Use
 * {@link CommonMessageReceiver#get()} to wait for the message.
 * 
 * @param <T>
 *            type of the message
 */
public class CommonMessageReceiver<T> implements MessageReceiver {

	private CountDownLatch latch = new CountDownLatch(1);
	private Object message = null;
	private AtomicBoolean cancelled = new AtomicBoolean(false);

	@Override
	public void receive(Object message) {
		this.message = message;
		this.latch.countDown();
	}

	@Override
	public void cancel() {
		this.cancelled.set(true);
		this.latch.countDown();
	}

	/**
	 * @return true if waiting was cancelled, false otherwise
	 */
	public boolean isCancelled() {
		return this.cancelled.get();
	}

	/**
	 * Get the message, waits until the message was received or waiting was
	 * cancelled
	 * 
	 * @return the message or null if waiting was cancelled, see
	 *         {@link CommonMessageReceiver#isCancelled()}
	 * @throws InterruptedException
	 *             if waiting was interrupted
	 */
	public T get() throws InterruptedException {
		this.latch.await();
		@SuppressWarnings("unchecked")
		T m = (T) message;
		return m;
	}

	/**
	 * Get the message, waits until the message was received, waiting was
	 * cancelled or the specified timeout was reached
	 * 
	 * @param timeout
	 *            the timeout to wait at most
	 * @param unit
	 *            the unit of the timeout
	 * @return the message or null if the timeout was reached or waiting was
	 *         cancelled, see {@link CommonMessageReceiver#isCancelled()}
	 * @throws InterruptedException
	 *             if waiting was interrupted
	 */
	public T get(long timeout, TimeUnit unit) throws InterruptedException {
		this.latch.await(timeout, unit);
		@SuppressWarnings("unchecked")
		T m = (T) message;
		return m;
	}

}
