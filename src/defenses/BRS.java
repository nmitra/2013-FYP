package defenses;
import java.util.ArrayList;
import java.util.Vector;


import distributions.BetaDistribution;
import distributions.PseudoRandom;

import main.Parameter;
import main.Product;
import main.Rating;
import agent.Buyer;
import agent.Seller;

public class BRS extends Defense{
	private double quantile = 0.01;
	private ArrayList<Boolean>trustAdvisors = new ArrayList<Boolean>();
	private double rep_aBS = 0.5;		




	private boolean iterative;

	public Seller chooseSeller(Buyer honestBuyer) {
		//calculate the trust values on target seller		
		ArrayList<Double> trustValues = new ArrayList<Double>();
		ArrayList<Double> mccValues = new ArrayList<Double>();
		Seller s = new Seller();
		for (int k = 0; k < 2; k++) {				
			int sid = Parameter.TARGET_DISHONEST_SELLER;
			if (k == 1)sid = Parameter.TARGET_HONEST_SELLER;
			trustValues.add(calculateTrust(s.getSeller(sid),honestBuyer));
			mccValues.add(calculateMCCofAdvisorTrust(sid));

		}
		//update the daily reputation difference
		//*****	ecommerce.updateDailyReputationDiff(trustValues);
		//*****	ecommerce.updateDailyMCC(mccValues);

		//select seller with the maximum trust values from the two target sellers
		int sellerid = Parameter.TARGET_DISHONEST_SELLER;
		if(trustValues.get(0) < trustValues.get(1)){
			sellerid = Parameter.TARGET_HONEST_SELLER;
		} else if (trustValues.get(0) == trustValues.get(1)){
			sellerid = (PseudoRandom.randDouble() < 0.5)?Parameter.TARGET_DISHONEST_SELLER:Parameter.TARGET_HONEST_SELLER;
		}
		if(PseudoRandom.randDouble() > Parameter.m_honestBuyerOntargetSellerRatio){ 
			sellerid = 1 + (int)(PseudoRandom.randDouble() * (Parameter.TARGET_DISHONEST_SELLER + Parameter.TARGET_HONEST_SELLER - 2));
		}		
		return s.getSeller(sellerid);
	}

	public double calculateTrust(Seller seller, Buyer honestBuyer){
		trustOfAdvisors = new ArrayList<Double>();
		int bid = honestBuyer.getId();
		double rep_aBS = 0.5;
		trustAdvisors = new ArrayList<Boolean>();

		//find buyers that have transaction with seller
		for (int j = 0; j < totalBuyers; j++) {
			int aid = j;
			if (aid == (Parameter.NO_OF_DISHONEST_BUYERS + Parameter.NO_OF_HONEST_BUYERS)) 
				break;
			trustAdvisors.set(aid, true);
			Buyer adv = new Buyer();		
			//search through buyer's transactions
			for (int i=0; i<adv.getTrans().size(); i++){
				boolean checkTrans= false;
				//transaction with seller exists
				if (adv.getTrans().get(i).getSeller().getId()==seller.getId()){
					checkTrans=true;
				}
				if (checkTrans==false){
					trustOfAdvisors.set(aid, 0.5);
					trustAdvisors.set(aid, false);
				}
			}

		}
		boolean iterative = true;
		do {
			iterative = false;
			//calculate reputation for seller based on all buyers
			calculateReputation1(honestBuyer, seller.getId());
			calculateReputation2(honestBuyer, seller.getId());
		} while(iterative=true);

		ArrayList<Integer> storedAdvisors = honestBuyer.getAdvisors();
		storedAdvisors.clear(); double bsr0=0; double bsr1=0;
		ArrayList<Double> np_BAforS = new ArrayList<Double>();	
		for (int j = 0; j < totalBuyers; j++) {
			int aid = j;
			if (aid == bid)continue;  //ignore its own rating
			if (trustAdvisors.set(aid, false))continue; //buyer no transaction with seller
			trustOfAdvisors.set(aid, 1.0);

			for(int i=0; i<honestBuyer.getBuyer(aid).getTrans().size(); i++){
				if(honestBuyer.getBuyer(aid).getTrans().get(i).getSeller().getId()==seller.getId() && i==0){
					bsr0 = np_BAforS.get(0) + honestBuyer.getBuyer(aid).getTrans().get(0).getRating().getCriteriaRatings().get(0).getCriteriaRatingValue();
				}
				if(honestBuyer.getBuyer(aid).getTrans().get(i).getSeller().getId()==seller.getId() && i==1){
					bsr1 = np_BAforS.get(1) + honestBuyer.getBuyer(aid).getTrans().get(1).getRating().getCriteriaRatings().get(1).getCriteriaRatingValue();
				}
			}
			np_BAforS.set(0, bsr0);
			np_BAforS.set(1, bsr1);

			//consider the trust of advisors to two target sellers in duopoly e-marketplaces
			storedAdvisors.add(aid);
			honestBuyer.setTrustAdvisor(aid, 1.0);
		}
		honestBuyer.calculateAverageTrusts(seller.getId());  //get the average trust of advisors based on seller
		rep_aBS = (np_BAforS.get(1) + 1.0 * Parameter.m_laplace) / (np_BAforS.get(0) + np_BAforS.get(1) + 2.0 * Parameter.m_laplace);

		return rep_aBS;
	}

	// step 1: calculate the reputation for seller based on all buyers.
	public void calculateReputation1(Buyer b, int sid){
		ArrayList<Double> BS_npSum = new ArrayList<Double>();
		double bsr0 =0; double bsr1=0;
		for (int j = 0; j < totalBuyers; j++) {
			int aid = j;
			if (aid == b.getId())continue;  //ignore its own rating
			if (trustAdvisors.get(aid) == false)continue;	//no transaction with seller	
			for(int i=0; i<b.getBuyer(aid).getTrans().size(); i++){
				if(b.getBuyer(aid).getTrans().get(i).getSeller().getId()==sid && i==0){
					bsr0 = BS_npSum.get(0) + b.getBuyer(aid).getTrans().get(0).getRating().getCriteriaRatings().get(0).getCriteriaRatingValue();
				}
				if(b.getBuyer(aid).getTrans().get(i).getSeller().getId()==sid && i==1){
					bsr1 = BS_npSum.get(1) + b.getBuyer(aid).getTrans().get(1).getRating().getCriteriaRatings().get(1).getCriteriaRatingValue();
				}
			}
			BS_npSum.set(0, bsr0);
			BS_npSum.set(1, bsr1);
			//BS_npSum[0] += BSR[aid][sid][0];
			//BS_npSum[1] += BSR[aid][sid][1];
		}
		rep_aBS = (BS_npSum.get(1) + 1.0) / (BS_npSum.get(0) + BS_npSum.get(1) + 2.0);
	}

	// step 2: calculate the reputation for seller based on one buyer		
	public void calculateReputation2(Buyer b, int sid){
		for (int j = 0; j < totalBuyers; j++) {
			int aid = j;
			double bsr0=0; double bsr1=0;
			if (trustAdvisors.get(aid)== false)continue;	

			for(int i=0; i<b.getBuyer(aid).getTrans().size(); i++){
				if(b.getBuyer(aid).getTrans().get(i).getSeller().getId()==sid && i==0){
					bsr0 = b.getBuyer(aid).getTrans().get(0).getRating().getCriteriaRatings().get(0).getCriteriaRatingValue();
				}
				if(b.getBuyer(aid).getTrans().get(i).getSeller().getId()==sid && i==1){
					bsr1 =b.getBuyer(aid).getTrans().get(1).getRating().getCriteriaRatings().get(1).getCriteriaRatingValue();
				}
			}

			BetaDistribution BDist_BrefS = new BetaDistribution((double) (bsr1 + 1), (double) (bsr0 + 1));
			double l = BDist_BrefS.getProbabilityOfQuantile(quantile);
			double u = BDist_BrefS.getProbabilityOfQuantile(1 - quantile);
			if (rep_aBS < l || rep_aBS > u) {
				// remove this buyer from the honest list
				trustAdvisors.set(aid, false);
				//since a buyer is removed from the list, reputation is calculated again (do while loop)
				iterative = true;
			}
		}
	}


}