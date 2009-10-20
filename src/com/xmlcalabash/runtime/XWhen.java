package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.*;

import java.util.Vector;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 4:57:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class XWhen extends XCompoundStep {
    public XWhen(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public boolean shouldRun() throws SaxonApiException {
        String testExpr = ((When) step).getTest();
        XdmNode doc = null;
        NamespaceBinding nsbinding = new NamespaceBinding(runtime, step.getNode());
        Hashtable<QName,RuntimeValue> globals = parent.getInScopeOptions();

        ReadablePipe reader = inputs.get("#xpath-context").firstElement();
        doc = reader.read();

        if (reader.moreDocuments() || inputs.get("#xpath-context").size() > 1) {
            throw XProcException.dynamicError(5);
        }
        
        Vector<XdmItem> results = evaluateXPath(doc, nsbinding.getNamespaceBindings(), testExpr, globals);

        if (results.size() > 1) {
            return true;
        }
        if (results.size() == 0) {
            return false;
        }

        XdmItem item = results.get(0);

        if (item.isAtomicValue()) {
            return ((XdmAtomicValue) item).getBooleanValue();
        } else {
            return true;
        }
    }

    protected void copyInputs() throws SaxonApiException {
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"#xpath-context".equals(port)) {
            String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments()) {
                        XdmNode doc = reader.read();
                        pipe.write(doc);
                        finest(step.getNode(), "Compound input copy from " + reader + " to " + pipe);
                    }
                }
            }
        }
    }    
}
