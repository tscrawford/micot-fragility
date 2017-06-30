package gov.lanl.micot.fragility.lpnorm;

import java.util.List;

public class RDTData {
	
	private List<RDTBuses> buses; 
	private List<RDTLoads> loads;
	private List<RDTGenerators> generators;
	private List<RDTLineCodes> line_codes;
	private List<RDTLines> lines;
	private String critical_load_met;
	private String total_load_met;
	private String chance_constraint;
	private String phase_variation;
	private List<RDTScenarios> scenarios;
	
	
	public RDTData() {}

	public List<RDTBuses> getBuses() {
		return buses;
	}

	public void setBuses(List<RDTBuses> buses) {
		this.buses = buses;
	}

	public List<RDTLoads> getLoads() {
		return loads;
	}

	public void setLoads(List<RDTLoads> loads) {
		this.loads = loads;
	}

	public List<RDTGenerators> getGenerators() {
		return generators;
	}

	public void setGenerators(List<RDTGenerators> generators) {
		this.generators = generators;
	}

	public List<RDTLineCodes> getLine_codes() {
		return line_codes;
	}

	public void setLine_codes(List<RDTLineCodes> line_codes) {
		this.line_codes = line_codes;
	}

	public String getCritical_load_met() {
		return critical_load_met;
	}

	public void setCritical_load_met(String critical_load_met) {
		this.critical_load_met = critical_load_met;
	}

	public String getTotal_load_met() {
		return total_load_met;
	}

	public void setTotal_load_met(String total_load_met) {
		this.total_load_met = total_load_met;
	}

	public String getChance_constraint() {
		return chance_constraint;
	}

	public void setChance_constraint(String chance_constraint) {
		this.chance_constraint = chance_constraint;
	}

	public String getPhase_variation() {
		return phase_variation;
	}

	public void setPhase_variation(String phase_variation) {
		this.phase_variation = phase_variation;
	}

	public List<RDTScenarios> getScenarios() {
		return scenarios;
	}

	public void setScenarios(List<RDTScenarios> scenarios) {
		this.scenarios = scenarios;
	}

	public List<RDTLines> getLines() {
		return lines;
	}

	public void setLines(List<RDTLines> lines) {
		this.lines = lines;
	}
	
	
}
