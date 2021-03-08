package havis.util.core.common.cycle;

import havis.util.cycle.ImplementationException;
import havis.util.cycle.Trigger;
import havis.util.cycle.TriggerCallback;
import havis.util.cycle.TriggerFactory;
import havis.util.cycle.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class TestTriggerFactory implements TriggerFactory {

	private List<Trigger> triggers = new ArrayList<>();

	@Override
	public Trigger create(String creatorId, final String uri, final TriggerCallback callback) throws ImplementationException, ValidationException {
		Trigger trigger = new Trigger() {
			@Override
			public boolean trigger() {
				return callback.invoke(this);
			}

			@Override
			public String getUri() {
				return uri;
			}

			@Override
			public void dispose() {
				triggers.remove(this);
			}
		};
		triggers.add(trigger);
		return trigger;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

}
