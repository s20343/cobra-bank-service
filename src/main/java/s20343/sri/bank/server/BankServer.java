package s20343.sri.bank.server;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import java.util.Properties;

public class BankServer {

    public static void main(String[] args) {
        try {
            System.out.println("[BankServer] Initializing ORB...");

            // point to the correct port
            Properties props = new Properties();
            props.put("org.omg.CORBA.ORBInitialPort", "2809");
            props.put("org.omg.CORBA.ORBInitialHost", "localhost");
            ORB orb = ORB.init(args, props);

            System.out.println("[BankServer] Initializing POA...");
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();

            System.out.println("[BankServer] Creating Servant...");
            BankServant bankServant = new BankServant();
            org.omg.CORBA.Object ref = rootPOA.servant_to_reference(bankServant);

            // REGISTER IN NAMING SERVICE
            System.out.println("[BankServer] Registering in Naming Service...");
            saveRefInNamingService(orb, ref, "BankService");

            System.out.println("[BankServer] Success: BankService registered. Waiting for requests...");
            orb.run();

        } catch (Exception e) {
            System.err.println("[BankServer] Critical failure:");
            e.printStackTrace();
        }
    }

    //Helper Method
    private static void saveRefInNamingService(ORB orb, org.omg.CORBA.Object ref, String refName) throws Exception {
        // 1. Obtaining a reference to a name service
        org.omg.CORBA.Object o = orb.resolve_initial_references("NameService");

        // 2. Mapping to a Java object
        NamingContextExt rootContext = NamingContextExtHelper.narrow(o);

        // 3. Creating a name component
        NameComponent nc = new NameComponent(refName, "");

        // 4. Registering the object
        NameComponent path[] = {nc};
        rootContext.rebind(path, ref);
    }
}