/*
 * Try.java
 *
 * Copyright 2008 Mark Logic Corporation.
 * Portions Copyright 2007 Sun Microsystems, Inc.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * https://xproc.dev.java.net/public/CDDL+GPL.html or
 * docs/CDDL+GPL.txt in the distribution. See the License for the
 * specific language governing permissions and limitations under the
 * License. When distributing the software, include this License Header
 * Notice in each file and include the License file at docs/CDDL+GPL.txt.
 */

package com.xmlcalabash.model;

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

/**
 *
 * @author ndw
 */
public class Try  extends DeclareStep {
    
    /** Creates a new instance of Try */
    public Try(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_try;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    @Override
    protected void augmentIO() {
        // Assume that everything will be OK when we validate...
        if (subpipeline.size() > 0) {
            Step step = subpipeline.get(0);
            for (Input input : step.inputs()) {
                Input cinput = new Input(xproc, step.getNode());
                cinput.setPort(input.getPort());
                cinput.setPrimary(input.getPrimary());
                addInput(cinput);
            }
            for (Output output : step.outputs()) {
                Output coutput = new Output(xproc, step.getNode());
                coutput.setPort(output.getPort());
                coutput.setPrimary(output.getPrimary());
                addOutput(coutput);
            }
        }
        super.augmentIO();
    }

    @Override
    protected void setEnvironment(Environment newEnvironment) {
        Environment env = new Environment(newEnvironment);

        // Now what about my subpipeline?
        for (Step step : subpipeline) {
            env.addStep(step);
        }
        
        patchEnvironment(env);
        super.setEnvironment(env);
        
        // Now what about my subpipeline
        for (Step step : subpipeline()) {
            Environment senv = new Environment(env);
            step.setEnvironment(senv);
        }
    }
    
    protected boolean validBindings() {
        boolean valid = true;
       
        // First, make sure all the substeps have the same bindings
        Hashtable<String,Input> inputs = new Hashtable<String,Input>();
        Hashtable<String,Output> outputs = new Hashtable<String,Output>();

         if (subpipeline.size() == 2) {
            Group p_group = (Group) subpipeline.get(0);
            for (Input input : p_group.inputs()) {
                inputs.put(input.getPort(), input);
            }
            for (Output output : p_group.outputs()) {
                outputs.put(output.getPort(), output);
            }

             Catch p_catch = (Catch) subpipeline.get(1);
             // there aren't any inputs here

            for (Output output : p_catch.outputs()) {
                if (outputs.containsKey(output.getPort())) {
                    Output s1output = outputs.get(output.getPort());
                    Output groupOut = getOutput(output.getPort());

                    // If any of the outputs can be a sequence, then the choose out can be a sequence
                    groupOut.setSequence(groupOut.getSequence() || output.getSequence());

                    if (output.getPort().endsWith("|") || output.getPort().startsWith("!")) {
                        // assume it's ok
                    } else {
                        if (s1output.getPrimary() != output.getPrimary()) {
                            valid = false;
                            xproc.error(logger, p_group.getNode(), "Output port " + output.getPort() + " has different primary status.", XProcConstants.staticError(7));
                        }
                        if (s1output.getSequence() != output.getSequence()) {
                            valid = false;
                            xproc.error(logger, p_group.getNode(), "Output port " + output.getPort() + " has different sequence status.", XProcConstants.staticError(7));
                        }
                    }
                } else {
                    valid = false;
                    xproc.error(logger, p_group.getNode(), "Output port " + output.getPort() + " is extra.", XProcConstants.staticError(7));
                }
            }
            for (String port : outputs.keySet()) {
                if (!port.endsWith("|") && p_group.getOutput(port) == null) {
                    valid = false;
                    xproc.error(logger, p_group.getNode(), "Output port " + port + " missing.", XProcConstants.staticError(7));
                }
            }
        } else {
           xproc.error(logger, getNode(), "Try must contain a group and a catch", XProcConstants.staticError(27));
        }

        return valid;
    }

    protected boolean validOutputBinding() {
        // The output of try is special.
        return true;
    }
}
