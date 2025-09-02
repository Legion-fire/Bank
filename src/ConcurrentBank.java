import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Потокобезопасный банк для управления счетами и переводами.
 */
class ConcurrentBank {
    private final ConcurrentMap<Long, BankAccount> accounts = new ConcurrentHashMap<>();

    /**
     * Создаёт новый счёт с уникальным номером и начальным балансом.
     */
    public BankAccount createAccount(long accountId, long initialBalance) {
        BankAccount acc = new BankAccount(accountId, initialBalance);
        BankAccount prev = accounts.putIfAbsent(accountId, acc);
        if (prev != null) {
            throw new IllegalArgumentException("Account already exists: " + accountId);
        }
        return acc;
    }

    /**
     * Перевод между счетами. Операции атомарные.
     */
    public void transfer(long fromAccountId, long toAccountId, long amount) {
        if (fromAccountId == toAccountId) {
            throw new IllegalArgumentException("from and to accounts must differ");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be > 0");
        }

        BankAccount from = accounts.get(fromAccountId);
        BankAccount to = accounts.get(toAccountId);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Unknown account(s): from=" + fromAccountId + ", to=" + toAccountId);
        }

        BankAccount first = from.getId() < to.getId() ? from : to;
        BankAccount second = from.getId() < to.getId() ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                // Внутри захвата обоих замков вызовы deposit/withdraw безопасны:
                // ReentrantLock позволит повторное захватывание тем же потоком.
                if (from == first) {
                    from.withdraw(amount);
                    to.deposit(amount);
                } else {
                    to.deposit(amount);
                    from.withdraw(amount);
                }
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    /**
     * Возвращает сумму балансов по всем счетам.
     * Для согласованного снимка блокируем все счета в порядке возрастания id.
     */
    public long getTotalBalance() {
        // Снимок текущих счетов
        List<BankAccount> snapshot = new ArrayList<>(accounts.values());
        snapshot.sort(Comparator.comparingLong(BankAccount::getId));

        // Захват всех замков в фиксированном порядке
        for (BankAccount acc : snapshot) {
            acc.getLock().lock();
        }

        try {
            long sum = 0L;
            for (BankAccount acc : snapshot) {
                sum = Math.addExact(sum, acc.getBalance());
            }
            return sum;
        } finally {
            // Освобождаем в обратном порядке
            for (int i = snapshot.size() - 1; i >= 0; i--) {
                snapshot.get(i).getLock().unlock();
            }
        }
    }

    public BankAccount getAccount(long accountId) {
        return accounts.get(accountId);
    }
}
