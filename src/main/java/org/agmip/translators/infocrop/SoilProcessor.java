package org.agmip.translators.infocrop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import org.agmip.common.Functions;
import org.agmip.util.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoilProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(SoilProcessor.class);
	private String outputDir;
	HashMap<String, SoilData> soilDataMap; 
	private final String NEW_LINE = "\r\n";
	
	
	public HashMap<String, SoilData> ProcessSoilData(String outputDir, HashMap<String, Object> results) throws IOException
	{
		this.outputDir = outputDir;
		
		
		this.soilDataMap = new HashMap<String, SoilData>();
		
		ArrayList<HashMap<String, Object>> soils = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(results, "soils", new HashMap<String, Object>());
		for(HashMap<String, Object> soil : soils) { generateSoilFile(soil); }
		
		LOG.info( "{}",soilDataMap );
		
		return soilDataMap;
	}
	
	public void generateSoilFile(HashMap<String, Object> soil)
	{
		StringBuffer recordData = new StringBuffer();
		String ksatm = MapUtil.getValueOr(soil, "sldr", "-9999");
		String soil_id = MapUtil.getValueOr(soil, "soil_id", "-9999");
		
		recordData.append("PARAM SLOPE").append("=").append(MapUtil.getValueOr(soil, "slpf", "-9999")).append(NEW_LINE);
		recordData.append("PARAM RUNOFF").append("=").append(MapUtil.getValueOr(soil, "slro", "-9999")).append(NEW_LINE);
		recordData.append("PARAM ALBEDO").append("=").append(MapUtil.getValueOr(soil, "salb", "-9999")).append(NEW_LINE);

		
		for(String slk: soil.keySet() ){ LOG.info("Soil Global Data {} {}", slk, soil.get(slk)); }
		
		
		generateSoilLayer(soil,ksatm,soil_id,recordData);
		LOG.debug(" Experiment {} \n{}",soil_id, recordData.toString());
		
		 
	}
	
	public void generateSoilLayer(HashMap<String, Object> soil,String ksatm, String soil_id,StringBuffer recordData)
	{
		
		int counter = 0;
		int MAX_LAYERS = 3, FIRST_LAYER = 1;
		ArrayList<HashMap<String, Object>> soilLayers = (ArrayList<HashMap<String, Object>>) MapUtil.getObjectOr(soil, "soilLayer", new ArrayList<HashMap<String, Object>>());
		BigInteger totalTKL = new BigInteger("0");
		soilDataMap.put(soil_id, new SoilData(recordData.toString()));
		SoilData soilDataObj = soilDataMap.get(soil_id);
		for(HashMap<String, Object> soilLayer : soilLayers) 
		{
			StringBuffer layerData = new StringBuffer();
			counter++;
			
			if( counter == FIRST_LAYER )
				recordData.append("PARAM PHSOL").append("=").append(MapUtil.getValueOr(soilLayer, "slphw", "-9999")).append(NEW_LINE);
			

			layerData.append(NEW_LINE).append("******************************SOIL LAYER").append(counter).append("******************************").append(NEW_LINE);
			layerData.append("PARAM KSATM").append(counter).append("=").append(ksatm).append(NEW_LINE);
			layerData.append("PARAM WCFCM").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "sldul", "-9999")).append(NEW_LINE);
			layerData.append("PARAM BDM").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "slbdm", "-9999")).append(NEW_LINE);
			layerData.append("PARAM SILT").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "slsil", "-9999")).append(NEW_LINE);
			
			if( counter == MAX_LAYERS )
				layerData.append("PARAM TKL").append(counter).append("M").append("=").append(MapUtil.getValueOr(soilLayer, "sllb", "-9999")).append(NEW_LINE);
			else
				layerData.append("PARAM TKL").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "sllb", "-9999")).append(NEW_LINE);
		
			layerData.append("PARAM WCSTM").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "slsat", "-9999")).append(NEW_LINE);
			layerData.append("PARAM OC").append(counter).append("IN").append("=").append(MapUtil.getValueOr(soilLayer, "sloc", "-9999")).append(NEW_LINE);
			layerData.append("PARAM CLAY").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "slcly", "-9999")).append(NEW_LINE);
			layerData.append("PARAM WCAD").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "slll", "-9999")).append(NEW_LINE);
			layerData.append("PARAM WCWP").append(counter).append("=").append(MapUtil.getValueOr(soilLayer, "slwp", "0")).append(NEW_LINE);

			
			soilDataObj.AddLayerData(layerData.toString());
			
			LOG.debug("===LayerData==== {} ", totalTKL.toString());
			for(String slk: soilLayer.keySet() ){ LOG.info("Layer Data {} {}", slk, soilLayer.get(slk)); }	
			
			if( counter == MAX_LAYERS ) break;
		}
		
		soilDataObj.SetGlobalData(recordData.toString());	 
	}
	
}
