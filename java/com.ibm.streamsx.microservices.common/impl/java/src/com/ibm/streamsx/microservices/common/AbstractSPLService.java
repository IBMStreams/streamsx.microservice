package com.ibm.streamsx.microservices.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.streamsx.topology.Topology;
import com.ibm.streamsx.topology.TopologyElement;
import com.ibm.streamsx.topology.spl.SPL;

public abstract class AbstractSPLService extends AbstractService {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AbstractSPLService() {
		super();
	}

	@Override
	protected Topology createTopology() {
	
		Topology topo = new Topology(getName());
	
		try {
			String[] dependencies = getToolkitDependencies();
			addToolkitDependencies(topo, dependencies);
	
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	
		Map<String, Object> params = getParameters();
	
		SPL.invokeOperator(topo, getName(), getMainCompositeFQN(), null, null, params);
	
		return topo;
	}

	protected Map<String, Object> getParameters() {
		Properties properties = getProperties();
		Map<String, Object> params = new HashMap<>();
		
		String paramPrefix = getMainCompositeFQN().replace("::", ".");
		
		Set<Object> keys = properties.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			if (key.startsWith(paramPrefix)){
				String param  = key.substring(paramPrefix.length()+1);
				params.put(param, properties.getProperty(key));
			}
		}
		return params;
	}

	protected String[] getToolkitDependencies() {
		// Add this toolkit as toolkit dependencies
		// Other dependencies should be handled by STREAMS_SPLPATH
		ArrayList<String> dependencies = new ArrayList<>();
		dependencies.add("./");
		return (String[])dependencies.toArray(new String[0]);
	}

	protected void addToolkitDependencies(TopologyElement element, String[] dependencies) throws IOException {
		for (String toolkit : dependencies) {
			SPL.addToolkit(element, new File(toolkit));
		}
	}
	
	protected String getName() {
		return getMainCompositeFQN().substring(getMainCompositeFQN().lastIndexOf("::") + 2) + "Wrapper";
	}
	
	
	protected abstract String getMainCompositeFQN();
	

}