package havis.util.cycle.common;

import havis.util.cycle.CycleData;

import java.util.ArrayList;
import java.util.List;

/**
 * Cycle data storing a simple list of data sets
 * 
 * @param <NotifyType>
 *            Type for a single data set added to the cycle data
 */
public class ListCycleData<NotifyType> implements CycleData<NotifyType> {

	private List<NotifyType> data = new ArrayList<>();

	@Override
	public void prepare() {
		reset();
	}

	@Override
	public void rotate() {
		// nothing to do
	}

	@Override
	public void reset() {
		this.data = new ArrayList<NotifyType>();
	}

	@Override
	public boolean add(NotifyType data) {
		return this.data.add(data);
	}

	/**
	 * @return the data
	 */
	public List<NotifyType> getData() {
		return this.data;
	}

	@Override
	public ListCycleData<NotifyType> clone() {
		ListCycleData<NotifyType> clone = new ListCycleData<>();
		clone.data.addAll(this.data);
		return clone;
	}

	@Override
	public void dispose() {
		// nothing to do
	}
}
