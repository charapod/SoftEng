package gov.nist.sip.proxy.softeng;

public class BillingFactory
{
	public static CostCalculator createCostCalculator(String strategy) {
		CostCalculator costCalculator = null;
		switch(strategy) {
		case Constants.CHARGE_FIXED_PRICE:
			costCalculator = new ChargeFixedPriceCalculator();
			break;
		case Constants.CHARGE_BY_RATE:
			costCalculator = new ChargeByRateCalculator();
			break;
		case Constants.FREE_OF_CHARGE:
			costCalculator = new FreeOfChargeCalculator();
			break;
		default:
			costCalculator = new ChargeByRateCalculator();
			break;
		}
		return costCalculator;
	}
}
