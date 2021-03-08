package havis.util.cycle;

/**
 * Factory for triggers
 */
public interface TriggerFactory {

	/**
	 * Create a trigger instance
	 * 
	 * @param creatorId
	 *            the ID of the creator
	 * @param uri
	 *            the URI of the trigger
	 * @param callback
	 *            the trigger callback
	 * @return the trigger instance
	 * @throws ImplementationException
	 *             if an internal error occurred
	 * @throws ValidationException
	 *             if validation failed
	 */
	public Trigger create(String creatorId, String uri, TriggerCallback callback) throws ImplementationException, ValidationException;
}
