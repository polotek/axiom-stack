/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile: JavaObject.java,v $
 * $Author: hannes $
 * $Revision: 1.16 $
 * $Date: 2005/09/21 10:11:10 $
 */

package axiom.scripting.rhino;

import org.mozilla.javascript.*;

import axiom.framework.ResponseTrans;
import axiom.framework.core.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 *
 */
public class JavaObject extends NativeJavaObject {

    RhinoCore core;
    String protoName;
    NativeJavaObject unscriptedJavaObj;

    static HashMap overload;

    static {
        overload = new HashMap();
        Method[] m = JavaObject.class.getMethods();
        for (int i=0; i<m.length; i++) {
            if ("href".equals(m[i].getName()) ||
                "renderSkin".equals(m[i].getName()) ||
                "renderSkinAsString".equals(m[i].getName())) {
                overload.put(m[i].getName(), m[i]);
            }
        }
    }

    /**
     *  Creates a new JavaObject wrapper.
     */
    public JavaObject(Scriptable scope, Object obj,
            String protoName, Scriptable prototype, RhinoCore core) {
        this.parent = scope;
        this.javaObject = obj;
        this.protoName = protoName;
        this.core = core;
        staticType = obj.getClass();
        unscriptedJavaObj = new NativeJavaObject(scope, obj, staticType);
        setPrototype(prototype);
        initMembers();
    }

    /**
     *
     *
     * @param action ...
     *
     * @return ...
     */
    public Object href(Object action) throws UnsupportedEncodingException, 
                                             IOException {
        if (javaObject == null) {
            return null;
        }

        String act = null;

        if (action != null) {
            if (action instanceof Wrapper) {
                act = ((Wrapper) action).unwrap().toString();
            } else if (!(action instanceof Undefined)) {
                act = action.toString();
            }
        }

        String basicHref = core.app.getNodeHref(javaObject, act, true);

        return core.postProcessHref(javaObject, protoName, basicHref);
    }

    /**
     * Checks whether the given property is defined in this object.
     */
    public boolean has(String name, Scriptable start) {
        // System.err.println ("HAS: "+name);
        if (overload.containsKey(name)) {
            return true;
        }
        return super.has(name, start);
    }

    /** 
     * Get a named property from this object.
     */
    public Object get(String name, Scriptable start) {
        // System.err.println ("GET: "+name);
        Object obj = overload.get(name);
        if (obj != null) {
            return new FunctionObject(name, (Method) obj, this);
        }

        // we really are not supposed to walk down the prototype chain in get(),
        // but we break the rule in order to be able to override java methods,
        // which are looked up by super.get() below
        Scriptable proto = getPrototype();
        while (proto != null) {
            obj = proto.get(name, start);
            if (obj != NOT_FOUND) {
                return obj;
            }
            proto = proto.getPrototype();
        }

        if ("_prototype".equals(name) || "__prototype__".equals(name)) {
            return protoName;
        }

        if ("__proto__".equals(name)) {
            return getPrototype();
        }

        if ("__javaObject__".equals(name)) {
            return unscriptedJavaObj;
        }

        return super.get(name, start);
    }

}
