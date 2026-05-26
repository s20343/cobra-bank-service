package s20343.sri.bank.server;
import bank.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BankServant extends BankServicePOA {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger accountCounter = new AtomicInteger(1001);
    private final AtomicInteger transactionCounter = new AtomicInteger(50001);

    public BankServant() {
        // Pre-populate with sample accounts as business model seed data
        initSampleAccount("John Doe", 5000.0);
        initSampleAccount("Alice Smith", 12500.50);
        initSampleAccount("Bob Miller", 350.0);
    }

    private void initSampleAccount(String owner, double balance) {
        String id = "ACC" + accountCounter.getAndIncrement();
        Account acc = new Account(id, owner, balance, "USD", "ACTIVE");
        accounts.put(id, acc);
    }

    @Override
    public Account createAccount(String owner, double initialDeposit) throws InvalidAmountException {
        if (owner == null || owner.trim().isEmpty()) {
            throw new InvalidAmountException("Owner name cannot be empty", 0.0);
        }
        if (initialDeposit < 0.0) {
            throw new InvalidAmountException("Initial deposit cannot be negative", initialDeposit);
        }

        String id = "ACC" + accountCounter.getAndIncrement();
        Account newAcc = new Account(id, owner, initialDeposit, "USD", "ACTIVE");
        accounts.put(id, newAcc);
        
        System.out.println("[BankServant] Created account: " + id + " for " + owner + " with " + initialDeposit + " USD.");
        return newAcc;
    }

    @Override
    public Account getAccount(String id) throws AccountNotFoundException {
        Account acc = accounts.get(id);
        if (acc == null) {
            throw new AccountNotFoundException("Account with ID " + id + " not found", id);
        }
        return acc;
    }

    @Override
    public Account deposit(String id, double amount) throws AccountNotFoundException, InvalidAmountException {
        if (amount <= 0.0) {
            throw new InvalidAmountException("Deposit amount must be strictly positive", amount);
        }

        Account acc = getAccount(id);
        double newBalance = acc.balance + amount;
        
        // Re-assign balance field in the CORBA Struct
        Account updatedAcc = new Account(acc.id, acc.owner, newBalance, acc.currency, acc.status);
        accounts.put(id, updatedAcc);

        System.out.println("[BankServant] Deposited " + amount + " USD to " + id + ". New balance: " + newBalance);
        return updatedAcc;
    }

    @Override
    public Account withdraw(String id, double amount) 
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
        
        if (amount <= 0.0) {
            throw new InvalidAmountException("Withdrawal amount must be strictly positive", amount);
        }

        Account acc = getAccount(id);
        if (acc.balance < amount) {
            throw new InsufficientFundsException("Insufficient funds in account " + id, amount, acc.balance);
        }

        double newBalance = acc.balance - amount;
        Account updatedAcc = new Account(acc.id, acc.owner, newBalance, acc.currency, acc.status);
        accounts.put(id, updatedAcc);

        System.out.println("[BankServant] Withdrew " + amount + " USD from " + id + ". New balance: " + newBalance);
        return updatedAcc;
    }

    @Override
    public TransferResult transfer(String fromId, String toId, double amount) 
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
        
        if (amount <= 0.0) {
            throw new InvalidAmountException("Transfer amount must be strictly positive", amount);
        }
        if (fromId.equals(toId)) {
            throw new InvalidAmountException("Cannot transfer to the same account", amount);
        }

        // Standard 2 phase verification of resources to simulate a safe transaction
        Account source = getAccount(fromId);
        Account destination = getAccount(toId);

        if (source.balance < amount) {
            throw new InsufficientFundsException("Transfer failed: Insufficient funds in source account " + fromId, amount, source.balance);
        }

        // Deduct and deposit
        double sourceNewBalance = source.balance - amount;
        Account updatedSource = new Account(source.id, source.owner, sourceNewBalance, source.currency, source.status);
        accounts.put(fromId, updatedSource);

        double destNewBalance = destination.balance + amount;
        Account updatedDest = new Account(destination.id, destination.owner, destNewBalance, destination.currency, destination.status);
        accounts.put(toId, updatedDest);

        String txId = "TX" + transactionCounter.getAndIncrement();
        long timestamp = System.currentTimeMillis();

        System.out.println("[BankServant] Transferred " + amount + " USD from " + fromId + " to " + toId + ". Transaction: " + txId);
        
        return new TransferResult(txId, updatedSource, updatedDest, amount, timestamp);
    }

    @Override
    public Account[] getAllAccounts() {
        Collection<Account> values = accounts.values();
        return values.toArray(new Account[0]);
    }
}
