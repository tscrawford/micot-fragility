package gov.lanl.micot.fragility.lpnorm;

import java.util.List;

public class RDTLines{

	private String id;
	private String node1_id;
	private String node2_id;
	private boolean[] has_phase;
	private float capacity;
	private float length;
	private int num_phases;
	private boolean is_transformer;
	private int line_code;
	private int num_poles;
	private float harden_cost;
	private float switch_cost;
	private boolean is_new;
	private boolean has_switch;
	private float construction_cost;

	public RDTLines() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNode1_id() {
		return node1_id;
	}

	public void setNode1_id(String node1_id) {
		this.node1_id = node1_id;
	}

	public String getNode2_id() {
		return node2_id;
	}

	public void setNode2_id(String node2_id) {
		this.node2_id = node2_id;
	}

	public float getCapacity() {
		return capacity;
	}

	public void setCapacity(float capacity) {
		this.capacity = capacity;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = length;
	}

	public int getNum_phases() {
		return num_phases;
	}

	public void setNum_phases(int num_phases) {
		this.num_phases = num_phases;
	}

	public boolean isIs_transformer() {
		return is_transformer;
	}

	public void setIs_transformer(boolean is_transformer) {
		this.is_transformer = is_transformer;
	}

	public int getLine_code() {
		return line_code;
	}

	public void setLine_code(int line_code) {
		this.line_code = line_code;
	}

	public int getNum_poles() {
		return num_poles;
	}

	public void setNum_poles(int num_poles) {
		this.num_poles = num_poles;
	}

	public float getHarden_cost() {
		return harden_cost;
	}

	public void setHarden_cost(float harden_cost) {
		this.harden_cost = harden_cost;
	}

	public float getSwitch_cost() {
		return switch_cost;
	}

	public void setSwitch_cost(float switch_cost) {
		this.switch_cost = switch_cost;
	}

	public boolean isIs_new() {
		return is_new;
	}

	public void setIs_new(boolean is_new) {
		this.is_new = is_new;
	}

	public boolean isHas_switch() {
		return has_switch;
	}

	public void setHas_switch(boolean has_switch) {
		this.has_switch = has_switch;
	}

	public boolean[] getHas_phase() {
		return has_phase;
	}

	public void setHas_phase(boolean[] has_phase) {
		this.has_phase = has_phase;
	}

	public float getConstruction_cost() {
		return construction_cost;
	}

	public void setConstruction_cost(float construction_cost) {
		this.construction_cost = construction_cost;
	}

}
