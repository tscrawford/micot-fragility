package gov.lanl.micot.fragility.lpnorm;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkDataStore {

	protected Map<String, NetworkLine> lines;
	protected Map<String, NetworkNode> nodes;

	public NetworkDataStore() {
		nodes = new HashMap<>();
		lines = new HashMap<>();
	}

	public void addLine(NetworkLine single) {
		String keyId = single.getId();
		lines.put(keyId, single);
	}

	public void addNode(NetworkNode single) {
		String keyId = single.getId();
		nodes.put(keyId, single);
	}

	public Collection<String> getLines() {
		return lines.keySet();
	}

	public Collection<String> getNodes() {
		return nodes.keySet();
	}

	public String getLineSource(String id) {
		return lines.get(id).getSource();
	}

	public String getLineTarget(String id) {
		return lines.get(id).getTarget();
	}

	public List<String> getNodeLineId(String id) {
		return nodes.get(id).getLineId();
	}

}
