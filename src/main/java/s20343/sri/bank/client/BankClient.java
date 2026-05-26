package s20343.sri.bank.client;

import bank.*;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import java.util.Properties;


public class BankClient {

    public static void main(String[] args) {
        try {
            System.out.println("==========================================================");
            System.out.println("  JACORB BANKING SYSTEM - CLIENT CONSOLE                  ");
            System.out.println("==========================================================");

            // 1. Initialize client's ORB with fixed port properties for stability
            Properties props = new Properties();
            props.put("org.omg.CORBA.ORBInitialPort", "2809");
            props.put("org.omg.CORBA.ORBInitialHost", "localhost");
            ORB orb = ORB.init(args, props);

            System.out.println("[BankClient] Locating Naming Service...");
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            System.out.println("[BankClient] Resolving 'BankService' handle...");
            BankService bank = BankServiceHelper.narrow(ncRef.resolve_str("BankService"));

            System.out.println("\n--- [1] CORE METHOD: Retrieving Pre-populated Accounts ---");
            Account[] initialAccounts = bank.getAllAccounts();
            for (Account acc : initialAccounts) {
                printAccount(acc);
            }

            System.out.println("\n--- [2] CORE METHOD: Creating a New Account ---");
            String clientName = "Murat Durmus";
            double initialCash = 1500.0;
            System.out.println("Creating account for: " + clientName + " (Initial: " + initialCash + " USD)");
            Account myAcc = bank.createAccount(clientName, initialCash);
            printAccount(myAcc);

            System.out.println("\n--- [3] CORE METHOD: Executing Deposits ---");
            System.out.println("Depositing 500.00 USD into: " + myAcc.id);
            myAcc = bank.deposit(myAcc.id, 500.0);
            printAccount(myAcc);

            System.out.println("\n--- [4] CORE METHOD: Executing Withdrawals ---");
            System.out.println("Withdrawing 250.00 USD from: " + myAcc.id);
            myAcc = bank.withdraw(myAcc.id, 250.0);
            printAccount(myAcc);

            System.out.println("\n--- [5] COMPLEX RPC: Account-to-Account Transfers ---");
            // Target the sample account created by server (John Doe is always ACC1001)
            String targetId = "ACC1001";
            double transferAmount = 800.0;
            System.out.println("Transferring " + transferAmount + " USD: " + myAcc.id + " -> " + targetId);

            TransferResult txResult = bank.transfer(myAcc.id, targetId, transferAmount);
            System.out.println(">>> TRANSACTION SUCCESSFUL");
            System.out.println("    Transaction ID : " + txResult.transactionId);
            System.out.println("    Sender Balance : " + txResult.sourceAccount.balance + " " + txResult.sourceAccount.currency);
            System.out.println("    Target Balance : " + txResult.destAccount.balance + " " + txResult.destAccount.currency);

            System.out.println("\n--- [6] EXCEPTION TEST: Insufficient Balance ---");
            try {
                System.out.println("Attempting to withdraw 100,000 USD from " + myAcc.id);
                bank.withdraw(myAcc.id, 100000.00);
            } catch (InsufficientFundsException ex) {
                System.out.println("EXPECTED EXCEPTION CAUGHT: " + ex.message);
                System.out.println("  Required: " + ex.requestedAmount + " | Available: " + ex.currentBalance);
            }

            System.out.println("\n--- [7] EXCEPTION TEST: Unknown Account Registry ---");
            try {
                System.out.println("Attempting to access non-existent account 'ACC9999'");
                bank.getAccount("ACC9999");
            } catch (AccountNotFoundException ex) {
                System.out.println("EXPECTED EXCEPTION CAUGHT: " + ex.message);
                System.out.println("  Offending ID: " + ex.invalidId);
            }

            System.out.println("\n==========================================================");
            System.out.println("  Assessment Tasks Completed Successfully                 ");
            System.out.println("==========================================================");

        } catch (Exception e) {
            System.err.println("[BankClient] Communication / Remote error:");
            e.printStackTrace();
        }
    }

    private static void printAccount(Account acc) {
        System.out.printf("  Account [%s] | Owner: %-15s | Balance: %8.2f %s | Status: %s\n",
                acc.id, acc.owner, acc.balance, acc.currency, acc.status);
    }
}