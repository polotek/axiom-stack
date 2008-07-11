package axiom.scripting.rhino.extensions.filter;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

import org.mozilla.javascript.*;

/**
 * @jsconstructor NotFilter
 */
public class NotFilterObject extends OpFilterObject {
    
    protected NotFilterObject() {
        super();
    }

    protected NotFilterObject(final Object[] args) throws Exception {
        super();
        filters = new IFilter[1];
        if (args.length >= 1) {
            if (args[0] instanceof IFilter)  {
                filters[0] = (IFilter) args[0];
            } else if (args[0] instanceof String) {
                filters[0] = new NativeFilterObject(args[0], null);
            } else if (args[0] instanceof Scriptable) {
                Scriptable s = (Scriptable) args[0];
                if (s.getClassName().equals("String")) {
                    filters[0] = new NativeFilterObject(s, null);
                } else {
                    filters[0] = new FilterObject(s, null, null);
                }
            } else {
                throw new Exception("Parameter to the NotFilter constructor is not a valid filter.");
            }
            
            if (args.length > 1 && args[1] instanceof Boolean) {
                this.cached = ((Boolean) args[1]).booleanValue();
            }
        } else {
            throw new Exception("No parameters specified to the NotFilter constructor.");
        }
    }
    
    public String getClassName() {
        return "NotFilter";
    }
    
    public String toString() {
        return "[NotFilter]";
    }
    
    public static NotFilterObject filterObjCtor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) 
    throws Exception {
        if (args.length == 0) {
            throw new Exception("No arguments specified to the NotFilter constructor.");
        }
        
        return new NotFilterObject(args);
    }
    
    public static void init(Scriptable scope) {
        Method[] methods = NotFilterObject.class.getMethods();
        ScriptableObject proto = new NotFilterObject();
        proto.setPrototype(getObjectPrototype(scope));
        final int attributes = READONLY | DONTENUM | PERMANENT;
        
        final int length = methods.length;
        for (int i = 0; i < length; i++) {
            String methodName = methods[i].getName();
            
            if ("filterObjCtor".equals(methodName)) {
                FunctionObject ctor = new FunctionObject("NotFilter", methods[i], scope);
                ctor.addAsConstructor(scope, proto);
            } else if (methodName.startsWith("jsFunction_")) {
                methodName = methodName.substring(11);
                FunctionObject func = new FunctionObject(methodName, methods[i], proto);
                proto.defineProperty(methodName, func, attributes);
            }
        }
    }
    
}