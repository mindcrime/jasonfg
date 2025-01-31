package jason.asSyntax.patterns.goal;

import jason.asSemantics.Agent;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Pred;
import jason.asSyntax.Term;
import jason.asSyntax.directives.DefaultDirective;
import jason.asSyntax.directives.Directive;
import jason.asSyntax.directives.DirectiveProcessor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Single-Minded Commitment pattern (see DALT 2006 paper)
 *
 * @author jomi
 */
public class SMC extends DefaultDirective implements Directive {

    static Logger logger = LoggerFactory.getLogger(SMC.class.getName());

    @Override
    public Agent process(Pred directive, Agent outerContent, Agent innerContent) {
        try {
            Term goal = directive.getTerm(0);
            Term fail = directive.getTerm(1);
            Pred subDir = Pred.parsePred("bc("+goal+")");
            //logger.debug("parameters="+goal+","+fail+","+subDir);
            Directive sd = DirectiveProcessor.getDirective(subDir.getFunctor());

            // apply sub directive
            Agent newAg = sd.process(subDir, outerContent, innerContent);
            if (newAg != null) {

                // add +f : true <- .fail_goal(g).
                newAg.getPL()
                    .add(ASSyntax.parsePlan("+"+fail+" <- .fail_goal("+goal+")."))
                    .setSourceFile(outerContent.getASLSrc());

                return newAg;
            }
        } catch (Exception e) {
            logger.error("Directive error.", e);
        }
        return null;
    }
}
