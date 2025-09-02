//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        ConcurrentBank bank = new ConcurrentBank();
        BankAccount account1 = bank.createAccount(1, 110);
        BankAccount account2 = bank.createAccount(2, 0);
        bank.getAccount(1).getBalance();
        bank.getAccount(2).getBalance();

        Thread transferThread1 = new Thread(() -> bank.transfer(account1.getId(), account2.getId(), 100));
        Thread transferThread2 = new Thread(() -> bank.transfer(account2.getId(), account1.getId(), 100));

        transferThread1.start();
        transferThread2.start();

        try {
            transferThread1.join();
            transferThread2.join();
        } catch (InterruptedException _) {

        }

        // Вывод общего баланса
        System.out.println("-----------------");
        System.out.println("Total balance: " + bank.getTotalBalance());

    }
}