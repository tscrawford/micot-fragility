package gov.lanl.micot.fragility.lpnorm;

public class NetworkLine extends NetworkTopology {
	
	private String id;
	private String source;
	private String target;
	
	public NetworkLine(){}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
