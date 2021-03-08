package havis.util.cycle;

import java.util.Date;

/**
 * Factory for reports
 * 
 * @param <NotifyType>
 *            Type for a single data set added to the cycle data
 * @param <CycleDataType>
 *            Type of cycle data, i.e. {@link SimpleCycleData}
 */
public interface ReportFactory<NotifyType, CycleDataType extends CycleData<NotifyType>> {

	/**
	 * Creates a report
	 * 
	 * @param data
	 *            the data to base the report on, can be null to request an
	 *            empty report, if this is the case, the returning report should
	 *            not be null
	 * @param date
	 *            the date of the report
	 * @param totalMilliseconds
	 *            the milliseconds of the cycle run
	 * @param initiation
	 *            the cycle initiation type
	 * @param initiator
	 *            the cycle initiator, e.g. the URI of a trigger
	 * @param termination
	 *            the cycle termination type
	 * @param terminator
	 *            the cycle terminator, e.g. the URI of a trigger
	 * @return the report, can be null
	 */
	Object create(CycleDataType data, Date date, long totalMilliseconds, Initiation initiation, String initiator, Termination termination, String terminator);

	/**
	 * Dispose the report factory
	 */
	void dispose();
}
