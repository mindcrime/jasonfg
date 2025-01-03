package jason.asSyntax.directives;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jason.asSyntax.Pred;
import jason.asSyntax.patterns.goal.BC;
import jason.asSyntax.patterns.goal.BDG;
import jason.asSyntax.patterns.goal.DG;
import jason.asSyntax.patterns.goal.EBDG;
import jason.asSyntax.patterns.goal.MG;
import jason.asSyntax.patterns.goal.OMC;
import jason.asSyntax.patterns.goal.RC;
import jason.asSyntax.patterns.goal.SGA;
import jason.asSyntax.patterns.goal.SMC;

/**
 * This class maintains the set of directives and is used by the
 * parser to process them.
 *
 * All available directives must be registered in this class using the
 * addDirective method.
 *
 * @author jomi
 *
 */
@SuppressWarnings("rawtypes")
public class DirectiveProcessor {
    static Logger logger = LoggerFactory.getLogger(DirectiveProcessor.class.getName());

    private static Map<String,Class> directives = new HashMap<String,Class>();
    private Map<String,Directive> instances  = new HashMap<String,Directive>();
    private static Map<String,Directive> singletons = new HashMap<String,Directive>();

    public static void registerDirective(String id, Class d) {
        directives.put(id,d);
    }

    @SuppressWarnings("unchecked")
    public static Directive getDirective(String id) {
        Directive d = singletons.get(id);
        if (d != null)
            return d;

        // create the instance
        Class c = directives.get(id);
        if (c == null) {
            logger.error( "Unknown directive "+id);
            return null;
        }

        try {
            d = (Directive)c.getConstructor().newInstance();
            if (d.isSingleton())
                singletons.put(id, d);
            return d;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //public static Directive removeDirective(String id) {
    //    return directives.remove(id);
    //}

    // add known directives
    static {
        registerDirective("include", Include.class);
        registerDirective("register_function", FunctionRegister.class);
        registerDirective("namespace", NameSpace.class);

        registerDirective("dg",  DG.class);
        registerDirective("bdg", BDG.class);
        registerDirective("ebdg",EBDG.class);
        registerDirective("bc",  BC.class);
        registerDirective("smc", SMC.class);
        registerDirective("rc",  RC.class);
        registerDirective("omc", OMC.class);
        registerDirective("mg",  MG.class);
        registerDirective("sga", SGA.class);
    }

    public Directive getInstance(Pred directive) {
        return getInstance(directive.getFunctor());
    }

    @SuppressWarnings("unchecked")
    public Directive getInstance(String id) {
        Directive d = instances.get(id);
        if (d != null)
            return d;

        d = singletons.get(id);
        if (d != null)
            return d;

        // create the instance
        Class c = directives.get(id);
        if (c == null) {
            logger.error( "Unknown directive "+id);
            return null;
        }

        try {
            d = (Directive)c.getConstructor().newInstance();
            if (d.isSingleton())
                singletons.put(id, d);
            else
                instances.put(id, d);
            return d;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
