import java.util.concurrent.locks.ReentrantLock;

/**
 * Исключение при недостатке средств.
 */
class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

/**
 * Потокобезопасный банковский счёт.
 * Баланс хранится в минимальных единицах.
 */
class BankAccount {

    private final long id;
    private long balance;
    private final ReentrantLock lock = new ReentrantLock();

    public BankAccount(long id, long initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance must be >= 0");
        }
        this.id = id;
        this.balance = initialBalance;
    }

    public long getId() {
        return id;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be > 0");
        }
        lock.lock();
        try {
            balance = Math.addExact(balance, amount);
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be > 0");
        }
        lock.lock();
        try {
            if (balance < amount) {
                throw new InsufficientFundsException("Insufficient funds on account " + id);
            }
            balance -= amount;
        } finally {
            lock.unlock();
        }
    }

    public long getBalance() {
        lock.lock();
        try {
            System.out.println(balance);
            return balance;
        } finally {
            lock.unlock();
        }
    }
}
