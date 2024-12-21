// Environment code for project big.mas2j

import jason.asSyntax.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BEnv extends jason.environment.Environment {

    private Logger logger = LoggerFactory.getLogger("big.mas2j."+BEnv.class.getName());

    @Override
    public boolean executeAction(String ag, Structure action) {
        logger.info("executing: "+action);
        return true;
    }
}

