package jjk;

import java.util.Properties;

import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.*;

public class App 
{
    public static void listNameService(org.omg.CORBA.Object objRef) throws InvalidName {
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
        BindingListHolder bl = new BindingListHolder();
        BindingIteratorHolder bi = new BindingIteratorHolder();            
        ncRef.list(1000, bl, bi);
        Binding bindings[] = bl.value;
        System.out.format("Amount: %d\n", bindings.length);
        for (int i=0; i < bindings.length; i++) {
            int lastIx = bindings[i].binding_name.length-1;
            // check to see if this is a naming context
            if (bindings[i].binding_type == BindingType.ncontext) {
                System.out.println( "Context: " + 
                bindings[i].binding_name[lastIx].id);
            } else {
                System.out.println("Object: " + 
                    bindings[i].binding_name[lastIx].id);
            }
        }
    }
    public static void main( String[] args ) throws InvalidName
    {
        String host = "<host>";
        String port = "<port>";
        Properties aProperties = new Properties();
        aProperties.put("org.omg.CORBA.ORBInitialHost", host);
        aProperties.put("org.omg.CORBA.ORBInitialPort", port);
        // create and initialize the ORB
        ORB orb = ORB.init(args, aProperties);
        String services[] = orb.list_initial_services();
        for (String service : services) {
            System.out.println(service);
            try {
                String uri = String.format("corbaloc::%s:%s/%s", host, port, service);
                if (service == "RootPOA") {
                    uri += "/Naming";
                }
                org.omg.CORBA.Object objRef = orb.string_to_object(uri);
                App.listNameService(objRef);
            } catch( org.omg.CORBA.OBJECT_NOT_EXIST e) {
                //Ignore
            }
        }
        //System.out.println("\nNameService:");
        //org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
    }
}
