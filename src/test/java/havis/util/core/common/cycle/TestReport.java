package havis.util.core.common.cycle;

public class TestReport {
	private String data;

	public TestReport() {
	}

	public TestReport(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "TestReport [data=" + data + "]";
	}
}
