package havis.util.cycle;

/**
 * Interface for collected data
 * 
 * @param <NotifyType>
 *            Type for a single data set added to the cycle data
 */
public interface CycleData<NotifyType> extends Cloneable {

	/**
	 * Prepare data at the start of a cycle, e.g. remove data that has timed out
	 */
	public void prepare();

	/**
	 * Rotate data at the end of a cycle, e.g. store current data for later
	 * comparison and reset current data
	 */
	public void rotate();

	/**
	 * Add new data
	 * 
	 * @param data
	 *            the data to add
	 * @return true if the data was new, false if it was already known and has
	 *         been updated
	 */
	public boolean add(NotifyType data);

	/**
	 * @return a clone of the data
	 */
	public CycleData<NotifyType> clone();

	/**
	 * Reset data when the cycle becomes unrequested, might wait for operations
	 * to finish
	 */
	public void reset();

	/**
	 * Dispose the data, cancel all waiting operations, if any
	 */
	public void dispose();
}
