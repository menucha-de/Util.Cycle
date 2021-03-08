package havis.util.cycle;

/**
 * Trigger
 */
public interface Trigger {

	/**
	 * @return the URI of the trigger
	 */
	public String getUri();

	/**
	 * Invoke the trigger
	 * 
	 * @return true if the trigger was successful, false if the trigger had no
	 *         effect on the trigger target, e.g. the cycle
	 */
	public boolean trigger();

	/**
	 * Dispose trigger
	 */
	public void dispose();
}
