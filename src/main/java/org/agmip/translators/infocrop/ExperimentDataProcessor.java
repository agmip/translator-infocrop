package org.agmip.translators.infocrop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.agmip.common.Functions;
import org.agmip.util.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentDataProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(ExperimentDataProcessor.class);
	private String outputDir;
	private String filePath;
	private final String NEW_LINE = "\r\n";
	private HashMap<String, SoilData> soilDataMap; 
	
	public void ProcessExperimentData(String outputDir, HashMap<String, Object> results,HashMap<String, SoilData> soilDataMap) throws IOException
	{
		this.outputDir = outputDir;
		this.soilDataMap = soilDataMap;

		ArrayList<HashMap<String, Object>> experiments = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(results, "experiments", new HashMap<String, Object>());
		for(HashMap<String, Object> experiment : experiments) { generateSoilFile(experiment); }
	}
	
	public void generateSoilFile(HashMap<String, Object> experiment)
	{
		StringBuffer recordData = new StringBuffer();
		String start_date = MapUtil.getValueOr(experiment, "sdat", "99990909");
		String exname = MapUtil.getValueOr(experiment, "exname", "-99");
		String soil_id = MapUtil.getValueOr(experiment, "soil_id", "-99");
		
		this.filePath = this.outputDir + "exp_" + exname;
		
		 Calendar cal = Calendar.getInstance();
		 cal.setTime(Functions.convertFromAgmipDateString(start_date));		
		
		 //LOG.info("#######START###########");
		//for(String exk: experiment.keySet() ){ LOG.info("Experiment Global Data {} - {}", exk, experiment.get(exk)); }
		 //LOG.info("Experiment Global Data {}", experiment.get("initial_conditions"));
		recordData.append("PARAM LAT=").append(MapUtil.getValueOr(experiment, "fl_lat", "-99")).append(NEW_LINE);
		recordData.append("PARAM LONG=").append(MapUtil.getValueOr(experiment, "fl_long", "-99")).append(NEW_LINE);
		recordData.append("PARAM YEAR=").append(cal.get(Calendar.YEAR)).append(NEW_LINE);
		recordData.append("PARAM START=").append(start_date).append(NEW_LINE);
		
		
		LOG.debug("GLOBAL Data {} {}",exname + " " + soil_id,  recordData );
		writeFile(recordData.toString());
		
		//HashMap<String, Object> obs_recs = (HashMap<String, Object>) MapUtil.getObjectOr(experiment, "observed", new HashMap<String, Object>());
		//for(String obs_rec: obs_recs.keySet() )  { LOG.info("Observed Data {} {}", obs_rec, obs_recs.get(obs_rec));  }
		
		
		HashMap<String, Object> mgmt_recs = (HashMap<String, Object>) MapUtil.getObjectOr(experiment, "management", new HashMap<String, Object>());
		//for(String mgmt_rec: mgmt_recs.keySet() ) {  LOG.info("MGMT Data {} {}", mgmt_rec, mgmt_recs.get(mgmt_rec)); }
	 	
		StringBuffer chemicalList = new StringBuffer();
		StringBuffer irrigationList = new StringBuffer();
		String prevIrrigationDate = "";
		 chemicalList.append("FUNCTION UREAP1=0.,0.,");
		 irrigationList.append("FUNCTION IRRTSF=0.,0.,");
	
		ArrayList<HashMap<String, Object>> event_recs = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(mgmt_recs, "events", new ArrayList<HashMap<String, Object>>());
	 	for(HashMap<String, Object> event_rec : event_recs) 
		{
	 		if( "irrigation".equals(  event_rec.get("event" ) ) )
	 		{
	 			String irrval = (String)event_rec.get("irval");
	 			String idas = (String)event_rec.get("date"); //irrigation date after sowing
	 			if( prevIrrigationDate != idas)
	 			{
		 			Calendar idas_cal = Calendar.getInstance();
		 			idas_cal.setTime(Functions.convertFromAgmipDateString(idas));
		 			long curr_days = daysBetween(cal,idas_cal);
		 			
		 			irrigationList.append((curr_days-1)).append(".,0.,");
		 			irrigationList.append(curr_days).append(".,").append(irrval).append(".,");
		 			irrigationList.append((curr_days+1)).append(".,0.,");
		 			//LOG.info(irrval + " " + idas + " " + curr_days );	 	
	 			}
	 			prevIrrigationDate = idas;
	 		}
	 		
	 		if( "fertilizer".equals(  event_rec.get("event" ) ) )
	 		{
	 			String feamn = (String)event_rec.get("feamn");
	 			String das = (String)event_rec.get("date"); //date after sowing
	 			Calendar das_cal = Calendar.getInstance();
	 			das_cal.setTime(Functions.convertFromAgmipDateString(das));		
	 			
	 			long curr_days = daysBetween(cal,das_cal);
	 			chemicalList.append((curr_days-1)).append(".,0.,");
	 			chemicalList.append(curr_days).append(".,").append(feamn).append(".,");
	 			chemicalList.append((curr_days+1)).append(".,0.,");
	 			
	 			
//		 		for(String erk: event_rec.keySet() )
//		 		{ 
//		 			LOG.info("MGMT Event Data {} {}",erk, event_rec.get(erk) );
//		 			//if( "fecd".equals(erk) )
//		 			//LOG.info(" Lookup {} ", LookupCodes.lookupCode(erk, event_rec.get(erk).toString(), "") );
//		 		}
	 		}
	 		
		}
	 	irrigationList.append("365.,0.");
	 	chemicalList.append("365.,0.");
	 	
	 	//LOG.info(foldString(chemicalList) );
	 	//LOG.info(foldString(irrigationList));
	 	
	 	writeFile(foldString(chemicalList));
	 	writeFile(foldString(irrigationList));

	 	
		HashMap<String, Object> initial_conditions = (HashMap<String, Object>) MapUtil.getObjectOr(experiment, "initial_conditions", new HashMap<String, Object>());
		//for(String ick: initial_conditions.keySet() ) {  LOG.info("InitData Data {} {}", ick, initial_conditions.get(ick)); }	 
		
		ArrayList<HashMap<String, Object>> soilLayers = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(initial_conditions, "soilLayer", new ArrayList<HashMap<String, Object>>());

		int soilCounter = 0;
		StringBuffer soilDataBuffer = new StringBuffer();
		
		SoilData soilData = SoilData.validateObject(soilDataMap.get(soil_id));
		writeFile(soilData.GetGlobalData());
			
 		
		for(HashMap<String, Object> soilLayer : soilLayers) 
		{
			soilCounter++;
			if( soilCounter < 4 )
			{
				soilDataBuffer.append( soilData.GetLayerData(soilCounter-1)  );
				soilDataBuffer.append("PARAM WCLI").append(soilCounter).append("=").append(soilLayer.get("ich2o")).append("\n");
			}
			//for(String slk: soilLayer.keySet() ){ LOG.info("Soil Layer Data {} {}",slk, soilLayer.get(slk) ); }	
		}
		LOG.debug(soilDataBuffer.toString());
		writeFile(soilDataBuffer.toString());
	}
	
	public String foldString(StringBuffer stringlist)
	{
		StringBuffer returnList = new StringBuffer();
		int lastCommaIndex = 0;
		int postfix = 0;
		for(int i=0; i<stringlist.length(); i++)
		{
			char currentChar = stringlist.charAt(i);
			
			if( currentChar == ',' )
				lastCommaIndex = i+1;
			
			returnList.append(currentChar);
			
			if( i!=0 && (i%65) == 0  )
			{
				returnList.insert(lastCommaIndex+postfix, "..."+NEW_LINE);
				postfix+=5;
			}
 		}
		return returnList.toString();
	}
		
	public static long daysBetween(final Calendar startDate, final Calendar endDate)
	{  
			 int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;  
			 long endInstant = endDate.getTimeInMillis();  
			 int presumedDays = (int) ((endInstant - startDate.getTimeInMillis()) / MILLIS_IN_DAY);  
			 Calendar cursor = (Calendar) startDate.clone();  
			 cursor.add(Calendar.DAY_OF_YEAR, presumedDays);  
			 long instant = cursor.getTimeInMillis();  
			 if (instant == endInstant)  
			  return presumedDays;  
			 final int step = instant < endInstant ? 1 : -1;  
			 do {  
			  cursor.add(Calendar.DAY_OF_MONTH, step);  
			  presumedDays += step;  
			 } while (cursor.getTimeInMillis() != endInstant);  
			 return presumedDays;  
	   }  	
	
	public void writeFile(String text)
	{
		try {
	          File file = new File(this.filePath);
	          
	          BufferedWriter output = new BufferedWriter(new FileWriter(file,true));
	         
	          output.write(text+NEW_LINE);
	          output.close();
	        } catch ( IOException e ) {
	           e.printStackTrace();
	        }
	}
}

/*
We need to split the chemicals currently everything is being assigned to urea

*/
