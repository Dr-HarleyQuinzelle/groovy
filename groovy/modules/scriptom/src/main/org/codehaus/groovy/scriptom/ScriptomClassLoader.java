package org.codehaus.groovy.scriptom;

import com.jacob.com.Variant;
import org.codehaus.groovy.classgen.ClassGenerator;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Type;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Custom class loader used to create a custom event handling interface with ASM.
 *
 * @author Guillaume Laforge
 */
public class ScriptomClassLoader extends ClassLoader
{
    private ClassLoader cl;
    private Map eventHandlers;

    /**
     * @param cl parent class loader
     * @param eventHandlers the eventHandlers Map
     */
    public ScriptomClassLoader(ClassLoader cl, Map eventHandlers)
    {
        this.cl = cl;
        this.eventHandlers = eventHandlers;
    }

    /**
     * Specific findClass() implementation which is responsible
     * of transforming the bytecode generated by ASM into an instance of Class.
     *
     * @param name of the class to find
     * @return a Class object
     * @throws ClassNotFoundException thrown if the Class was not found
     */
    protected Class findClass(String name) throws ClassNotFoundException
    {
        if (name.endsWith("EventHandler"))
        {
            byte[] bytes = generateInterface();
            return super.defineClass("EventHandler", bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }

    /**
     * Use ASM to generate a custom <code>EventHandler</code> interface,
     * according to the eventHandlers Map of <code>EventSupport</code>
     * which contains the event handling code.
     *
     * @return a byte array representing the java bytecode of the interface
     */
    private byte[] generateInterface()
    {
        // create an interface
        ClassWriter cw = new ClassWriter(false);
        cw.visit(ClassGenerator.asmJDKVersion, Constants.ACC_PUBLIC + Constants.ACC_ABSTRACT + Constants.ACC_INTERFACE,
                 "EventHandler", "java/lang/Object", (String[]) null, "EventHandler.java");

        // for each closure in the Map...
        Set set = eventHandlers.keySet();
        for (Iterator iterator = set.iterator(); iterator.hasNext();)
        {
            String eventName = (String) iterator.next();
            // create a method of signature: public void eventName(Variant[] variants)
            cw.visitMethod(Constants.ACC_PUBLIC + Constants.ACC_ABSTRACT, eventName,
                           Type.getMethodDescriptor(Type.getType(Void.TYPE), new Type[]{Type.getType(Variant[].class)}),
                           (String[]) null, (Attribute) null);
        }

        // we're done with the generation!
        cw.visitEnd();

        // return the bytecode of the created interface
        return cw.toByteArray();
    }

}
