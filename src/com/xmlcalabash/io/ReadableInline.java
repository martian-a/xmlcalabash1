/*
 * ReadableInline.java
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

package com.xmlcalabash.io;

import java.util.Vector;
import java.util.HashSet;
import java.util.logging.Logger;

import net.sf.saxon.s9api.*;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.Step;

/**
 *
 * @author ndw
 */
public class ReadableInline implements ReadablePipe {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;
    private boolean readSeqOk = false;
    private int pos = 0;
    private Step reader = null;

    /** Creates a new instance of ReadableInline */
    public ReadableInline(XProcRuntime runtime, Vector<XdmValue> nodes, HashSet<String> excludeNS) {
        this.runtime = runtime;
        documents = new DocumentSequence(runtime);
        XdmDestination dest = new XdmDestination();

        // Find the document element so we can get the base URI
        XdmNode node = null;
        for (int pos = 0; pos < nodes.size() && node == null; pos++) {
            if (((XdmNode) nodes.get(pos)).getNodeKind() == XdmNodeKind.ELEMENT) {
                node = (XdmNode) nodes.get(pos);
            }
        }

        try {
            S9apiUtils.writeXdmValue(runtime, nodes, dest, node.getBaseURI());
            XdmNode doc = dest.getXdmNode();

            doc = S9apiUtils.removeNamespaces(runtime, doc, excludeNS);
            runtime.finest(logger, null, "Instantiate a ReadableInline");
            documents.add(doc);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }

    public void canReadSequence(boolean sequence) {
        readSeqOk = sequence;
    }
    
    public void resetReader() {
        pos = 0;
    }
    
    public boolean moreDocuments() {
        return pos < documents.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        return documents.size();
    }

    public DocumentSequence documents() {
        return documents;
    }

    public void setReader(Step step) {
        reader = step;
    }

    public XdmNode read() throws SaxonApiException {
        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            runtime.finest(logger, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }
        
        return doc;
    }

    public String toString() {
        return "readableinline " + documents;
    }
}
