package havis.util.cycle;

/**
 * Callback for triggers
 */
public interface TriggerCallback {
	/**
	 * Invoke a trigger
	 * 
	 * @param trigger
	 *            the trigger to invoke
	 * @return true if triggering had a effect of the triggered service, false
	 *         otherwise
	 */
	public boolean invoke(Trigger trigger);
}
