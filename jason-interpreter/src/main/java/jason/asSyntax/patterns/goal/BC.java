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
 * Implementation of the Blind Commitment pattern (see DALT 2006 paper)
 *
 * @author jomi
 */
public class BC extends DefaultDirective implements Directive {

    static Logger logger = LoggerFactory.getLogger(BC.class.getName());

    @Override
    public Agent process(Pred directive, Agent outerContent, Agent innerContent) {
        try {
            Term goal = directive.getTerm(0);
            Pred subDir;
            if (directive.getArity() > 1) {
                subDir = Pred.parsePred(directive.getTerm(1).toString());
            } else {
                subDir = Pred.parsePred("bdg("+goal+")");
            }
            Directive sd = DirectiveProcessor.getDirective(subDir.getFunctor());

            // apply sub directive
            Agent newAg = sd.process((Pred)subDir, outerContent, innerContent);
            if (newAg != null) {

                // add +!g : true <- !g.
                newAg.getPL()
                    .add(ASSyntax.parsePlan("+!"+goal+" <- !"+goal+"."))
                    .setSourceFile(outerContent.getASLSrc());

                return newAg;
            }
        } catch (Exception e) {
            logger.error("Directive error.", e);
        }
        return null;
    }
}
