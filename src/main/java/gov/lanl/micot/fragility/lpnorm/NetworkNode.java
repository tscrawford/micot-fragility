package gov.lanl.micot.fragility.lpnorm;

import java.util.List;

public class NetworkNode extends NetworkTopology {
	
	private String id;
	private String label;
	private List<String> lineId;
	
	public NetworkNode(){}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getLabel(){
		return label;
	}
	
	public void setLabel(String label){
		this.label = label;
	}

	public NetworkNode getSource() {
		return this;
	}

	public List<String> getLineId() {
		return lineId;
	}

	public void setLineId(List<String> lineId) {
		this.lineId = lineId;
	}

	
	

}
