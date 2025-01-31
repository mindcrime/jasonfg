package jason.infra.jade;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jason.control.ExecutionControl;
import jason.control.ExecutionControlInfraTier;
import jason.mas2j.ClassParameters;
import jason.runtime.RuntimeServices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;

/**
 * Concrete execution control implementation for Jade infrastructure.
 */
@SuppressWarnings("serial")
public class JadeExecutionControl extends JadeAg implements ExecutionControlInfraTier {

    public static String controllerOntology = "AS-ExecControl";

    private ExecutionControl userControl;
    private ExecutorService executor; // the thread pool used to execute actions

    @Override
    public void setup()  {
        logger = LoggerFactory.getLogger(JadeExecutionControl.class.getName());

        // create the user environment
        try {
            Object[] args = getArguments();
            if (args != null && args.length > 0) {
                if (args[0] instanceof ClassParameters) { // it is an mas2j parameter
                    ClassParameters ecp = (ClassParameters)args[0];
                    userControl = (ExecutionControl) Class.forName(ecp.getClassName()).getConstructor().newInstance();
                    userControl.setExecutionControlInfraTier(this);
                    userControl.init(ecp.getParametersArray());
                } else {
                    userControl = (ExecutionControl) Class.forName(args[0].toString()).getConstructor().newInstance();
                    userControl.setExecutionControlInfraTier(this);
                    if (args.length > 1) {
                        logger.warn("Execution control arguments is not implemented yet (ask it to us if you need)!");
                    }
                }
            } else {
                logger.warn("Using default execution control.");
                userControl = new ExecutionControl();
                userControl.setExecutionControlInfraTier(this);
            }
        } catch (Exception e) {
            logger.error( "Error in setup Jade Environment", e);
        }

        executor = Executors.newFixedThreadPool(5);

        try {
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    userControl.updateNumberOfAgents();
                    informAllAgsToPerformCycle(0);
                    /*
                    executor.execute(new Runnable() {
                        public void run() {
                        }
                    });
                    */
                }
            });

            addBehaviour(new CyclicBehaviour() {
                ACLMessage m;
                public void action() {
                    m = receive();
                    if (m == null) {
                        block(1000);
                    } else {
                        try {
                            // check if the message is an agent state
                            @SuppressWarnings("unused")
                            Document o = (Document)m.getContentObject();
                            logger.warn("Received agState too late! in-reply-to:"+m.getInReplyTo());
                        } catch (Exception ex0) {
                            try {
                                // check if the message is an end of cycle from some agent
                                final String content = m.getContent();
                                final int p = content.indexOf(",");
                                if (p > 0) {
                                    final String sender  = m.getSender().getLocalName();
                                    final boolean breakpoint = Boolean.parseBoolean(content.substring(0,p));
                                    final int cycle = Integer.parseInt(content.substring(p+1));
                                    executor.execute(new Runnable() {
                                        public void run() {
                                            try {
                                                userControl.receiveFinishedCycle(sender, breakpoint, cycle);
                                            } catch (Exception e) {
                                                logger.error( "Error processing end of cycle.", e);
                                            }
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                logger.error( "Error in processing "+m, e);
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            logger.error( "Error starting agent", e);
        }
    }

    @Override
    protected void takeDown() {
        if (userControl != null) userControl.stop();
    }

    public ExecutionControl getUserControl() {
        return userControl;
    }

    public void informAgToPerformCycle(final String agName, final int cycle) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                m.setOntology(controllerOntology);
                m.addReceiver(new AID(agName, AID.ISLOCALNAME));
                m.setContent("performCycle");
                m.addUserDefinedParameter("cycle", String.valueOf(cycle));
                send(m);
            }
        });
    }

    public void informAllAgsToPerformCycle(final int cycle) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                try {
                    logger.debug("Sending performCycle "+cycle+" to all agents.");
                    ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                    m.setOntology(controllerOntology);
                    addAllAgsAsReceivers(m);
                    m.setContent("performCycle");
                    m.addUserDefinedParameter("cycle", String.valueOf(cycle));
                    send(m);
                } catch (Exception e) {
                    logger.error( "Error in informAllAgsToPerformCycle", e);
                }
            }
        });
    }

    public Document getAgState(final String agName) {
        if (agName == null) return null;

        state = null;
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                try {
                    ACLMessage m = new ACLMessage(ACLMessage.QUERY_REF);
                    m.setOntology(controllerOntology);
                    m.addReceiver(new AID(agName, AID.ISLOCALNAME));
                    m.setContent("agState");
                    ACLMessage r = ask(m);
                    if (r == null) {
                        System.err.println("No agent state received! (possibly timeout in ask)");
                    } else {
                        state = (Document) r.getContentObject();
                    }
                } catch (Exception e) {
                    logger.error( "Error in getAgState", e);
                } finally {
                    synchronized (syncWaitState) {
                        syncWaitState.notifyAll();
                    }
                }
            }
        });
        return waitState();
    }

    private Document state = null;
    private Object syncWaitState = new Object();
    private Document waitState() {
        if (state == null) {
            synchronized (syncWaitState) {
                try {
                    syncWaitState.wait();
                } catch (InterruptedException e) {}
            }
        }
        return state;
    }

    public RuntimeServices getRuntimeServices() {
        return new JadeRuntimeServices(getContainerController(), this);
    }
}
