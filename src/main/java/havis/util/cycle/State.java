package havis.util.cycle;

public enum State {

	/**
	 * Cycle is undefined
	 * 
	 */
	UNDEFINED,

	/**
	 * Cycle is unrequested
	 * 
	 */
	UNREQUESTED,

	/**
	 * Cycle is requested
	 * 
	 */
	REQUESTED,

	/**
	 * Cycle is active
	 * 
	 */
	ACTIVE;

	public String value() {
		return name();
	}

	public static State fromValue(String v) {
		return valueOf(v);
	}
}
