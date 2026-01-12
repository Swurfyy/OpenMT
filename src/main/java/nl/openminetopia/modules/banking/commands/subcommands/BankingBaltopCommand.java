package nl.openminetopia.modules.banking.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.enums.AccountType;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandAlias("accounts|account|rekening")
public class BankingBaltopCommand extends BaseCommand {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("M/d/yy, h:mm a", Locale.US);
    private static final int PAGE_SIZE = 8;

    @Subcommand("baltop")
    @CommandPermission("openminetopia.banking.baltop")
    public void baltop(Player player, @Optional Integer page) {
        BankingModule bankingModule = OpenMinetopia.getModuleManager().get(BankingModule.class);
        
        // Filter only COMPANY and SAVINGS accounts, exclude PRIVATE
        List<BankAccountModel> filteredAccounts = bankingModule.getBankAccountModels().stream()
                .filter(account -> account.getType() == AccountType.COMPANY || account.getType() == AccountType.SAVINGS)
                .sorted(Comparator.comparing(BankAccountModel::getBalance).reversed())
                .collect(Collectors.toList());

        if (filteredAccounts.isEmpty()) {
            ChatUtils.sendMessage(player, "<red>Er zijn geen COMPANY of SAVINGS accounts gevonden.");
            return;
        }

        // Calculate total balance for COMPANY & SAVINGS accounts
        double serverTotal = filteredAccounts.stream()
                .mapToDouble(BankAccountModel::getBalance)
                .sum();

        // Calculate pagination
        int totalPages = (int) Math.ceil(filteredAccounts.size() / (double) PAGE_SIZE);
        int currentPage = (page == null || page < 1) ? 1 : Math.min(page, totalPages);

        if (currentPage < 1 || currentPage > totalPages) {
            ChatUtils.sendMessage(player, "<red>Deze pagina bestaat niet.");
            return;
        }

        // Show loading message
        ChatUtils.sendMessage(player, "<gold>Ordering balances of " + filteredAccounts.size() + " accounts, please wait...");

        // Format date/time
        String dateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        // Show header
        ChatUtils.sendMessage(player, "<yellow>Top balances (" + dateTime + ")");
        ChatUtils.sendMessage(player, "<yellow>-------------------------------- <gold>Balancetop <yellow>-------------------------------- <red>Page " + currentPage + "/" + totalPages);

        // Show server total
        ChatUtils.sendMessage(player, "<yellow>Server Total: <red>" + bankingModule.format(serverTotal));

        // Calculate pagination indices
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredAccounts.size());

        // Show account list
        for (int i = startIndex; i < endIndex; i++) {
            BankAccountModel account = filteredAccounts.get(i);
            int rank = i + 1;
            ChatUtils.sendMessage(player, "<white>" + rank + ". " + account.getName() + ", " + bankingModule.format(account.getBalance()));
        }

        // Show navigation instruction
        if (totalPages > 1) {
            if (currentPage < totalPages) {
                ChatUtils.sendMessage(player, "<gold>Type /accounts baltop " + (currentPage + 1) + " to read the next page.");
            } else {
                ChatUtils.sendMessage(player, "<gold>Type /accounts baltop 1 to read the first page.");
            }
        }
    }
}
