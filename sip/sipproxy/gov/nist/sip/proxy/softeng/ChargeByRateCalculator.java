package gov.nist.sip.proxy.softeng;

public class ChargeByRateCalculator extends CostCalculator {
	private long rate = Constants.RATE;

	@Override
	public long calculateCost() {
		System.out.println("Calc ByRate");
		System.out.println("Start = " + timeStart);
		System.out.println("End = " + timeEnd);
		long callTime = (this.timeEnd - this.timeStart)/1000;
		return rate * callTime;
	}
}
