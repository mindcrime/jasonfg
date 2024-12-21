import jason.asSyntax.Structure;

import org.slf4j.Logger;

public class BEnv extends jason.environment.Environment {

    @Override
    public boolean executeAction(String ag, Structure action) {
        return true;
    }
}
