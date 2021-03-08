package havis.util.cycle;

/**
 * Type of termination condition
 */
public enum Termination {

	/**
	 * Unknown
	 */
	NULL,

	/**
	 * Cycle was undefined
	 */
	UNDEFINE,

	/**
	 * A trigger occurred
	 */
	TRIGGER,

	/**
	 * Duration time expired
	 */
	DURATION,

	/**
	 * Interval time expired
	 */
	INTERVAL,

	/**
	 * New data available
	 */
	DATA_AVAILABLE,

	/**
	 * State of cycle changed to unrequested
	 */
	UNREQUESTED,

	/**
	 * Error occurred
	 */
	ERROR;

	/**
	 * Returns the termination condition as string
	 * 
	 * @param termination
	 *            The termination condition
	 * @return The string representation of the termination condition
	 */
	public static String toString(Termination termination) {
		return termination == null ? null : termination.name();
	}
}