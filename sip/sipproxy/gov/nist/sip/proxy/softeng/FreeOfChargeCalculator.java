package gov.nist.sip.proxy.softeng;

public class FreeOfChargeCalculator extends CostCalculator {

	@Override
	public long calculateCost() {
		System.out.println("Calc Free");
		return 0;
	}

}
