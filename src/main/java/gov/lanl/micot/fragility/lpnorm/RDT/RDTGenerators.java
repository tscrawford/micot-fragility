package gov.lanl.micot.fragility.lpnorm;

public class RDTGenerators {

	private String id;
	private String node_id;
	private boolean[] has_phase;
	private float[] max_real_phase;
	private float[] max_reactive_phase;
	private float microgrid_cost;
	private float microgrid_fixed_cost;
	private float max_microgrid;
	private boolean is_new;

	public RDTGenerators() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNode_id() {
		return node_id;
	}

	public void setNode_id(String node_id) {
		this.node_id = node_id;
	}

	public boolean[] getHas_phase() {
		return has_phase;
	}

	public void setHas_phase(boolean[] has_phase) {
		this.has_phase = has_phase;
	}

	public float[] getMax_real_phase() {
		return max_real_phase;
	}

	public void setMax_real_phase(float[] max_real_phase) {
		this.max_real_phase = max_real_phase;
	}

	public float[] getMax_reactive_phase() {
		return max_reactive_phase;
	}

	public void setMax_reactive_phase(float[] max_reactive_phase) {
		this.max_reactive_phase = max_reactive_phase;
	}

	public float getMicrogrid_cost() {
		return microgrid_cost;
	}

	public void setMicrogrid_cost(float microgrid_cost) {
		this.microgrid_cost = microgrid_cost;
	}

	public float getMicrogrid_fixed_cost() {
		return microgrid_fixed_cost;
	}

	public void setMicrogrid_fixed_cost(float microgrid_fixed_cost) {
		this.microgrid_fixed_cost = microgrid_fixed_cost;
	}

	public float getMax_microgrid() {
		return max_microgrid;
	}

	public void setMax_microgrid(float max_microgrid) {
		this.max_microgrid = max_microgrid;
	}

	public boolean isIs_new() {
		return is_new;
	}

	public void setIs_new(boolean is_new) {
		this.is_new = is_new;
	}

}
