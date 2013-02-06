package org.agmip.translators.infocrop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.agmip.common.Functions;
import org.agmip.util.JSONAdapter;
import org.agmip.util.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(WeatherProcessor.class);
	private String outputDir;
	private int stationIndex = 0;
	public void ProcessWeatherData(String outputDir, HashMap<String, Object> results) throws IOException
	{
		this.outputDir = outputDir;

		ArrayList<HashMap<String, Object>> weathers = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(results, "weathers", new HashMap<String, Object>());
		for(HashMap<String, Object> wst : weathers) { generateWeatherFile(wst); }
	}

	
	public void generateWeatherFile(HashMap<String, Object> wst)
	{
		String arngstrom_const_a="0.25",  arngstrom_const_b="0.5";
		String lat =  MapUtil.getValueOr(wst, "wst_lat", "-99");
		String lng =  MapUtil.getValueOr(wst, "wst_long", "-99");
		String elevation =  MapUtil.getValueOr(wst, "wst_elev", "0");
		String wst_id =  MapUtil.getValueOr(wst, "wst_id", "-99");
		
		//for(String wst1: wst.keySet() ){ LOG.info("Global Data {}", wst1);}
		
		StringBuffer recordData = new StringBuffer();
		recordData.append(lng).append(" ").append(lat).append(" ").append(elevation).append(" ").append(arngstrom_const_a).append(" ").append(arngstrom_const_b).append(" ");
		LOG.debug("Global Data {} {}",wst_id, recordData.toString());
		generateDay(wst,wst_id,recordData.toString());
	}
	
	
	
	public void generateDay(HashMap<String, Object> wst,String station_number,String headerData)
	{
		//String station_number, String year, String julian_day, String solar_radiation, String min_temp, String max_temp, String vapor_pressure, String wind_speed, String rain_fall
		stationIndex++;
		ArrayList<HashMap<String, Object>> dailyWeather = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(wst, "dailyWeather", new ArrayList<HashMap<String, Object>>());
		
		for(HashMap<String, Object> todaysWeather : dailyWeather) 
		{
			StringBuffer recordData = new StringBuffer();
			//for(String wst1: todaysWeather.keySet() ){ LOG.info("Today's Data {} {}",wst1, todaysWeather.get(wst1) ); }	
			
			 
			 Calendar cal = Calendar.getInstance();
			 cal.setTime(Functions.convertFromAgmipDateString(MapUtil.getValueOr(todaysWeather, "w_date", "99990909")));
			 String solar_radiation = Functions.multiply(MapUtil.getValueOr(todaysWeather, "srad", "0"),"1000");
			 
			recordData.append(stationIndex).append(" ")
			.append(cal.get(Calendar.YEAR)).append(" ").append(cal.get(Calendar.DAY_OF_YEAR)).append(" ")
			.append(solar_radiation).append(" ")
			.append(MapUtil.getValueOr(todaysWeather, "tmin", "-9999")).append(" ")
			.append(MapUtil.getValueOr(todaysWeather, "tmax", "-9999")).append(" ")
			.append(MapUtil.getValueOr(todaysWeather, "vprsd", "0")).append(" ")
			.append(MapUtil.getValueOr(todaysWeather, "wind", "0")).append(" ")
			.append(MapUtil.getValueOr(todaysWeather, "rain", "0")).append(" ");
			
			String fileName = outputDir+station_number.substring(0,3)+stationIndex+"."+(new Integer(cal.get(Calendar.YEAR))).toString().substring(1);
			LOG.debug("Data Record {}", recordData.toString()  );
			
			writeFile(fileName,recordData.toString(),headerData);
			//break;
		}	
	
	}
	
	public void writeFile(String filePath,String text,String headerRecord)
	{
		try {
	          File file = new File(filePath);
	          Boolean bWriteHeader = !file.exists();
	          
	          BufferedWriter output = new BufferedWriter(new FileWriter(file,true));
	          if(bWriteHeader)
	        	  output.write(headerRecord+"\r\n");
	          
	          output.write(text+"\r\n");
	          output.close();
	        } catch ( IOException e ) {
	           e.printStackTrace();
	        }
	}
	

}
