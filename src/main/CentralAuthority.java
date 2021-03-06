package main;

import agent.*;

import java.text.DecimalFormat;
import java.util.*;
import java.io.*;
import java.lang.*;
import distributions.*;
import environment.*;
import main.*;

public class CentralAuthority {

	private Environment env;
	private String m_defenseName;
	private String m_attackName;
	private ArrayList<Buyer> m_buyers = new ArrayList<Buyer>();
	private ArrayList<Seller> m_sellers = new ArrayList<Seller>();

	//sellers sales, reputation, reputation difference per day
	//    private int[][] m_dailySales;
	//    private double[][] m_dailyRep;
	//    private double[][] m_dailyRepDiff;
	//Defense model cost time per day
	//  private double[] defenseTime_day;
	ArrayList<Double> defenseTime_day= new ArrayList<Double>();
	ArrayList<Double> honest_avgWt;
	ArrayList<Double> dishonest_avgWt;
	//    private double[][] m_dailyAvgWeights;
	//    private double[][] m_dailyMCC;

	ArrayList transList;
	public CentralAuthority(){          
		//            m_dailySales = new int[Parameter.NO_OF_DAYS + 1][Parameter.NO_OF_DISHONEST_SELLERS + Parameter.NO_OF_HONEST_SELLERS];
		//            m_dailyRep  = new double[Parameter.NO_OF_DAYS + 1][2];
		//            m_dailyMCC= new double[Parameter.NO_OF_DAYS + 1][2];
		//            m_dailyRepDiff  = new double[Parameter.NO_OF_DAYS + 1][2];
		//            defenseTime_day = new double[Parameter.NO_OF_DAYS + 1];
		//            m_dailyAvgWeights = new double[Parameter.NO_OF_DAYS + 1][2];
		honest_avgWt = new ArrayList();
		dishonest_avgWt = new ArrayList();
		env = new EnvironmentS();
		for(int i=0; i<Parameter.NO_OF_DAYS; i++){
			defenseTime_day.add(i,0.0);
			dishonest_avgWt.add(i,0.0);
			honest_avgWt.add(i,0.0);

		}
	}


	public void setUpEnvironment(String attackName, String defenseName)throws Exception{
		//System.out.println("enters setUp Environment");
		env.eCommerceSetting(attackName, defenseName);
		//  env.agentSetting(attackName, defenseName);
	}

	public ArrayList simulateEnvironment(String attackName, String defenseName, boolean dailyPrint) throws ClassNotFoundException, NoSuchMethodException, SecurityException,Exception{


		System.out.println("enters simulateEnvironment "+attackName+"  "+defenseName);
		//step 1: initialize initialize the e commerce
		m_attackName = new String(attackName);
		m_defenseName = new String(defenseName);
		setUpEnvironment(attackName, defenseName);      
		transList = new ArrayList();
		//TODO discuss about the name specification
		m_buyers=env.getBuyerList();
		m_sellers=env.getSellerList();

		//Day 0 is the eve of Day 1 (only for agents' decision making), transactions start on Day 1             
		for (int day = 0; day < Parameter.NO_OF_DAYS; day++){                                                   
			if (day != 0){
				for (int count = 0; count<m_buyers.size();count++){
					m_buyers.get(count).setCredits(m_buyers.get(count).getCredits()+Parameter.CREDITS_PER_TURN);
				}
			}

			for (int a = 0; a<PseudoRandom.randInt(1, Parameter.transaction_limit);a++){
				DecimalFormat roundoff = new DecimalFormat("#.##");
				//Transaction trans = new Transaction();
				//  trans.setDay(day);
			//	m_buyers.get(a).addTransaction(day);

				/*       double cost = trans.getQuantity()*trans.getPrice();
                cost = Double.valueOf(roundoff.format(cost));
                if (cost>m_buyers.get(trans.getBuyer().getId()).getCredits()){
                    trans.setRemarks("Transaction Failed: Buyer not enough Credit");
                } else {
                    trans.setRemarks("Transaction Successful");
                    m_buyers.get(trans.getBuyer().getId()).setCredits(m_buyers.get(trans.getBuyer().getId()).getCredits()-cost);
                }
                transList.add(trans);
				 */
				}

			//step 2: Attack model (dishonest buyers)               
			attack(day);            

			//step 3: Defense model (honest buyers)
			long defensetimeStart = new Date().getTime();
			defense(day);   
			long defensetimeEnd = new Date().getTime();
			//TODO how to store transaction
			//    defenseTime_day[day] = (-defensetimeStart + defensetimeEnd) / 1000.0;
			defenseTime_day.set(day,(-defensetimeStart + defensetimeEnd) / 1000.0 );
			//            m_dailySales = m_eCommerce.getDailySales();
			//            m_dailyRep[day] = m_eCommerce.getDailyReputation();
			//            m_dailyRepDiff[day] = m_eCommerce.getDailyReputationDiff();
			//            m_dailyMCC[day] = m_eCommerce.getDailyMCC();
			env.setDay(day); //update to next day
			avgerWeights(day);
			if(dailyPrint){
				//print the transactions for different sellers
				System.out.print("Day " + day + ": ");                  
				System.out.print("   |sellers' transactions|: ");
				for (int i = 0; i < Parameter.NO_OF_HONEST_SELLERS   + Parameter.NO_OF_DISHONEST_SELLERS; i++) {
					if(i == Parameter.NO_OF_DISHONEST_SELLERS){
						System.out.print(", ");
					}                       
					//                  System.out.print(" " + m_dailySales[day][i]);
				}       
				//                System.out.print("   |rep MAE|: " + m_dailyRep[day][0] + "  " + m_dailyRep[day][1]);
				//                System.out.print("   |repDiff MAE|: " + m_dailyRepDiff[day][0] + "  " + m_dailyRepDiff[day][1]);
				// System.out.println(" avgWeights: " + m_dailyAvgWeights[day][0] + "  " + m_dailyAvgWeights[day][1]);
				System.out.println("avg. weights for dishonest: "+ dishonest_avgWt.get(day) +"  avg. weights for honest "+honest_avgWt.get(day));
                              
			}
		/*HashMap<Seller,Double> maeComp = new HashMap();
                for(int i=0;i<m_buyers.size();i++){
                   maeComp = m_buyers.get(i).getMAEList();
                  for(int j=0;j<m_sellers.size();j++){
                   System.out.println("MAE of seller "+m_sellers.get(j) +" "+maeComp.get(m_sellers.get(j)));
                  }

                }*/
 /* for(int i=0; i<m_buyers.size(); i++){
                                    m_buyers.get(i).setDay(day+1);
                               }*/
                      System.out.println("MAE for day " + day + " is " + env.getDailyRepDiff().get(0) + " " + env.getDailyRepDiff().get(1));
                     // System.out.println("MCC for day " + day + " is " + env.getDailyMCC().get(0) + " " + env.getDailyMCC().get(1));
                }

		return transList;
	}

	private void attack(int day){

		int numBuyers = Parameter.NO_OF_DISHONEST_BUYERS + Parameter.NO_OF_HONEST_BUYERS;
		//Attack model (dishonest buyers), give rating/ perform attack              
		/*  for(int i = 0; i < numBuyers; i++){
            int bid = i;
            if(m_buyers.get(bid).isIshonest() == false){
                m_buyers.get(bid).giveRating(day);
            }
        }*/
		m_buyers = env.getBuyerList();
//System.out.println(m_buyers.get(index))
		for(int i = 0; i < numBuyers; i++){
			int bid = i;
			if(m_buyers.get(bid).isIshonest() == false){
				// m_buyers.get(bid).perform_model(day);
				m_buyers.get(bid).addTransaction(day);
                             // try{
                             //     env.createARFFfile();
                             // }
                             // catch(Exception e){}


                        }
		}
	}

	private void defense(int day){

		int numBuyers = Parameter.NO_OF_DISHONEST_BUYERS + Parameter.NO_OF_HONEST_BUYERS;
		//Attack model (dishonest buyers), give rating/ perform defense             
		/* for(int i = 0; i < numBuyers; i++){
            int bid = i;
            if(m_buyers.get(bid).isIshonest() == true){
                m_buyers.get(bid).giveRating(day);
            }
        }
		 * /
		 */
		for(int i = 0; i < numBuyers; i++){
			int bid = i;
			if(m_buyers.get(bid).isIshonest() == true){
				m_buyers.get(bid).addTransaction(day);
			}
		}
             //   m_buyers.get(0).calMAE(day);
              /*    for(int i=0; i<m_buyers.size(); i++){
                                    m_buyers.get(i).setDay(day+1);
                                }*/
	}

	private void avgerWeights(int day){

		int db = Parameter.NO_OF_DISHONEST_BUYERS;
		int hb = Parameter.NO_OF_HONEST_BUYERS; 
		//  if(Parameter.includeWhitewashing()){
		//    db = db + (day) * db;
		//}       

		//these code for trust models: trustworthiness for local/partial advisors;
		if(Parameter.includeWMA(m_defenseName) || Parameter.includeEA(m_defenseName)){
			int numDA = 0; //number of dishonest advisors;
			int numHA = 0; //number of honest advisors
			honest_avgWt.set(day,(double)0);
			honest_avgWt.set(day, (double)0);


			for(int i = Parameter.NO_OF_DISHONEST_BUYERS; i < Parameter.NO_OF_DISHONEST_BUYERS + hb; i++){
				int bid = i;

				ArrayList<Double> weights_BA = new ArrayList<Double>();
				// weights_BA.add(m_buyers.get(bid).getTrusts());
				for (int j=0; j<m_buyers.get(bid).getTrusts().size(); j++){
					weights_BA.add(m_buyers.get(bid).getTrusts().get(j));
				}
				weights_BA.add(m_buyers.get(bid).getTrusts().get(i));
				ArrayList<Buyer> advisors = new ArrayList<Buyer>();
				// advisors.add(m_buyers.get(bid).getAdvisors());
				for (int j=0;j<m_buyers.get(bid).getAdvisors().size();j++){


					advisors.add(m_buyers.get(bid).getAdvisor(j));

				}
				for(int j = 0; j < advisors.size(); j++){                    
					int aid = advisors.get(j).getId(); double oldWt;
					if(aid == bid)continue;
					if(aid < Parameter.NO_OF_DISHONEST_BUYERS || aid >= Parameter.NO_OF_DISHONEST_BUYERS + hb){
						// m_dailyAvgWeights[day][0] += weights_BA.get(aid);

						oldWt = dishonest_avgWt.get(day);
						oldWt +=weights_BA.get(aid);
						dishonest_avgWt.set(day, oldWt);
						numDA++;

					} else{
						oldWt = honest_avgWt.get(day);
						oldWt +=weights_BA.get(aid);
						honest_avgWt.set(day, oldWt);
						numHA++;
					}
				}

			}
			if (numDA != 0) {
				//  double wt =  (dishonest_avgWt.get(day))/ numDA;
				dishonest_avgWt.set(day,(dishonest_avgWt.get(day)/ numDA));
				// m_dailyAvgWeights[day][0] /= (numDA);
			}           
			if (numHA != 0) {
				// m_dailyAvgWeights[day][1] /= (numHA);
				honest_avgWt.set(day,(honest_avgWt.get(day)/numHA));
			}
		}
		else{
			//these code for trust models: trustworthiness for all advisors;
			int numDA = 0;
			int numHA = 0;
			for(int i = Parameter.NO_OF_DISHONEST_BUYERS; i < Parameter.NO_OF_DISHONEST_BUYERS + hb; i++){
				int bid = i;                
				for (int k = 0; k < 2; k++) {                
					int sid = Parameter.TARGET_DISHONEST_SELLER;
					if (k == 1)sid = Parameter.TARGET_HONEST_SELLER;
					ArrayList <Double> SaverTA = new ArrayList<Double>();
					//  SaverTA.add(m_buyers.get(bid).getAverageTrusts(sid));

					double array1 [] =new double[m_buyers.get(bid).getAverageTrusts(sid).length]; //check
					array1 = m_buyers.get(bid).getAverageTrusts(sid); //check
					for(int j=0;j<array1.length;j++)  //check
						SaverTA.add(array1[j]);  //check

					if(SaverTA.get(0) >= 0){//dishonest advisors
						//System.out.println(dishonest_avgWt.get(day));
						dishonest_avgWt.set(day,dishonest_avgWt.get(day) +SaverTA.get(0));
						//m_dailyAvgWeights[day][0] += SaverTA[0];
						numDA++;
					}
					if(SaverTA.get(1) >= 0){//dishonest advisors
						honest_avgWt.set(day, honest_avgWt.get(day)+SaverTA.get(1));
						// m_dailyAvgWeights[day][1] += SaverTA[1];
						numHA++;
					}
				}   
			}       

			if (numDA != 0) {
				dishonest_avgWt.set(day,(dishonest_avgWt.get(day)/ numDA));
				//m_dailyAvgWeights[day][0] /= (numDA);
			}           
			if (numHA != 0) {
				honest_avgWt.set(day,(honest_avgWt.get(day)/ numDA));
				// m_dailyAvgWeights[day][1] /= (numHA);
			}   
		}       

	}

	public void evaluateDefenses(ArrayList<String> defenseNames, ArrayList<String> attackNames, String evaluateName) throws Exception,ClassNotFoundException, NoSuchMethodException, SecurityException{

		int runtimes = Parameter.NO_OF_RUNTIMES;                    //runtimes =  50
		transList = new ArrayList();
		//output the result: [|transactions|, time]
		//      double[][][][] results = new double[runtimes][defenseNames.length][attackNames.length][2];
		for(int i = 0; i < 1; i++){
			for(int j = 0; j < defenseNames.size(); j++){            
				for(int k = 0; k < attackNames.size(); k++){                 
					//            System.err.print("  runtimes = " + i + ",   defense = " + defenseNames[j] + ",   attack = " + attackNames[k]);


					long start = new Date().getTime();
					// [true / false] means print/not daily result     
					transList = simulateEnvironment(attackNames.get(k), defenseNames.get(j), false);
					long end = new Date().getTime();
					System.err.println("   time =  " + (end - start)/1000.0 + " s");
					//TODO ADD LATER: results[i][j][k] = generate_report(i); //report include the run count                 
				}
			}           
		}

		//TODO ADD LATER: getStatistic(results, defenseNames, attackNames, evaluateName);

	}


	public void showOutput(){

	}

	public void exportToDB(){

	}

	public void importFromDB(){

	}

	public void getCentralReputation(){

	}

	public void displaySuggestions(){

	}


}