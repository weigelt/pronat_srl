package edu.kit.ipd.parse.srlabeler;

import java.util.Properties;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.data.AbstractPipelineData;
import edu.kit.ipd.parse.luna.pipeline.IPipelineStage;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

@MetaInfServices(IPipelineStage.class)
public class SRLabeler implements IPipelineStage {
	
	private static final Logger logger = LoggerFactory.getLogger(SRLabeler.class);

	private static final String ID = "srl";
	
	private Properties props;
	

	@Override
	public void init() {
		props = ConfigManager.getConfiguration(getClass());
		
	}

	@Override
	public void exec(AbstractPipelineData data) throws PipelineStageException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getID() {
		return ID;
	}

}
