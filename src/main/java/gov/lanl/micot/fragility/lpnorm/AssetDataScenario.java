package gov.lanl.micot.fragility.lpnorm;

import gov.lanl.nisac.fragility.io.AssetData;

public class AssetDataScenario extends AssetData{
	
	private String scenario_id;
	
	public AssetDataScenario(){}

	public String getScenario_id() {
		return scenario_id;
	}

	public void setScenario_id(String scenario_id) {
		this.scenario_id = scenario_id;
	}
	
}
