package gov.nist.sip.proxy.softeng;

public class ChargeFixedPriceCalculator extends CostCalculator {
	private long fixedCost = Constants.FIXED_PRICE;

	@Override
	public long calculateCost() {
		System.out.println("Calc Fixed");
		return fixedCost;
	}

}
