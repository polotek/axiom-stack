package axiom.scripting.rhino.extensions.filter;

import java.lang.reflect.*;
    
import org.mozilla.javascript.*;

/**
 * @jsconstructor Sort
 */
public class SortObject extends ScriptableObject {
    
    private QuerySortField[] sortFields = null;
    
    public static final int ASCENDING = 0;
    public static final int DESCENDING = 1;
    
    protected SortObject() {
        super();
    }
    
    public SortObject(final Object arg) {
        super();
        if (arg instanceof String) {
            sortFields = new QuerySortField[1];
            sortFields[0]= new QuerySortField((String) arg, ASCENDING);
        } else if (arg instanceof NativeArray) { 
            NativeArray na = (NativeArray) arg;
            int length = (int) na.getLength();
            sortFields = new QuerySortField[length];
            
            for (int i = 0; i < length; i++) {
                Object o = na.get(i, na);
                if (o instanceof ScriptableObject) {
                    ScriptableObject so = (ScriptableObject) o;
                    final String id = ScriptableObject.getPropertyIds(so)[0].toString();
                    final String sorder = ScriptableObject.getProperty(so, id).toString().toLowerCase();
                    int order = ASCENDING;
                    if ("desc".equals(sorder) || "des".equals(sorder) || "dec".equals(sorder)) {
                        order = DESCENDING;
                    }
                    sortFields[i] = new QuerySortField(id, order);
                }
            }
        } else if (arg instanceof ScriptableObject) {
            ScriptableObject s = (ScriptableObject) arg;
            final Object[] ids = ScriptableObject.getPropertyIds(s);
            final int length = ids.length;
            sortFields = new QuerySortField[length];
            
            for (int i = 0; i < length; i++) {
                final String id = ids[i].toString();
                final String curr = ScriptableObject.getProperty(s, id).toString().toLowerCase();
                int order = ASCENDING;
                if ("desc".equals(curr) || "des".equals(curr) || "dec".equals(curr)) {
                    order = DESCENDING;
                }
                sortFields[i] = new QuerySortField(id, order);
            }            
        }
    }
    
    public String getClassName() {
        return "Sort";
    }
    
    public String toString() {
        return "[Sort]";
    }
    
    public static SortObject sortObjCtor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) 
    throws Exception {
        if (args.length == 0) {
            throw new Exception("No arguments specified to the Sort constructor.");
        }
        
        if (args[0] == null) {
            throw new Exception("Null argument specified to the Sort constructor.");
        }
        
        return new SortObject(args[0]);
    }
    
    public static void init(Scriptable scope) throws PropertyException {
        Method[] methods = SortObject.class.getDeclaredMethods();
        ScriptableObject proto = new SortObject();
        proto.setPrototype(getObjectPrototype(scope));
        
        final int attributes = READONLY | DONTENUM | PERMANENT;
        
        final int length = methods.length;
        for (int i = 0; i < length; i++) {
            String methodName = methods[i].getName();
            if ("sortObjCtor".equals(methodName)) {
                FunctionObject ctor = new FunctionObject("Sort", methods[i], scope);
                ctor.addAsConstructor(scope, proto);
                break;
            } 
        }
    }
    
    public QuerySortField[] getSortFields() {
        return this.sortFields;
    }
    
    public static String getRelationalSortValue(final int order) {
        String sort = "ASC";
        switch (order) {
        case ASCENDING: sort = "ASC"; break;
        case DESCENDING: sort = "DESC"; break;
        default: sort = "ASC"; break;
        }
        return sort;
    }
    
}