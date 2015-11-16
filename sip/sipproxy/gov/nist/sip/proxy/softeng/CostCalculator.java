package gov.nist.sip.proxy.softeng;

public abstract class CostCalculator {
	protected long timeStart;
	protected long timeEnd;

	public long getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(long timeStart) {
		this.timeStart = timeStart;
	}

	public long getTimeEnd() {
		return timeEnd;
	}

	public void setTimeEnd(long timeEnd) {
		this.timeEnd = timeEnd;
	}
	
	public abstract long calculateCost();
	
}
