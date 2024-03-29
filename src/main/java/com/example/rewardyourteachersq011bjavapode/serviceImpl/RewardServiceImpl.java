package com.example.rewardyourteachersq011bjavapode.serviceImpl;

import com.example.rewardyourteachersq011bjavapode.dto.InitializeTransactionRequest;
import com.example.rewardyourteachersq011bjavapode.enums.NotificationType;
import com.example.rewardyourteachersq011bjavapode.enums.Role;
import com.example.rewardyourteachersq011bjavapode.enums.TransactionType;
import com.example.rewardyourteachersq011bjavapode.exceptions.UserNotFoundException;
import com.example.rewardyourteachersq011bjavapode.exceptions.WalletNotFoundException;
import com.example.rewardyourteachersq011bjavapode.models.User;
import com.example.rewardyourteachersq011bjavapode.models.Wallet;
import com.example.rewardyourteachersq011bjavapode.repository.TransactionRepository;
import com.example.rewardyourteachersq011bjavapode.repository.UserRepository;
import com.example.rewardyourteachersq011bjavapode.repository.WalletRepository;
import com.example.rewardyourteachersq011bjavapode.response.ApiResponse;
import com.example.rewardyourteachersq011bjavapode.service.RewardService;
import com.example.rewardyourteachersq011bjavapode.service.TransactionService;
import com.example.rewardyourteachersq011bjavapode.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RewardServiceImpl implements RewardService {

    private final WalletRepository walletRepository;
    private final UserUtil userUtil;
    private final NotificationServiceImpl notificationService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final UUID uuid = UUID.randomUUID();
    @Override
    public ApiResponse<String> rewardTeacherByTeacherId(Long receiverID ,  InitializeTransactionRequest request) throws WalletNotFoundException {
        BigDecimal amount = request.getAmount();
        User sender = userUtil.getUserByEmail(userUtil.getAuthenticatedUserEmail());
        User receiver = userRepository.findUserById(receiverID).orElseThrow(() -> new UserNotFoundException("User not found"));
        Wallet receiversWallet = walletRepository.findWalletByUserId(receiverID).orElseThrow(()-> new WalletNotFoundException("Wallet not found"));
        Wallet sendersWallet = walletRepository.findWalletByUserEmail(userUtil.getAuthenticatedUserEmail()).orElseThrow(()-> new WalletNotFoundException("Wallet not found"));
        if (receiver.getRole().equals(Role.STUDENT)){
            String message = "You are not allowed to send reward to a student";
            return new ApiResponse<>("failed", LocalDateTime.now(), message);
        }
        if (sendersWallet.getBalance().compareTo(amount) >= 0 ){
            sendersWallet.setBalance(sendersWallet.getBalance().subtract(amount));
            receiversWallet.setBalance(receiversWallet.getBalance().add(amount));
            walletRepository.saveAll(List.of(sendersWallet, receiversWallet));
            notificationService.saveNotification(receiverID , "You received ₦" + amount + " from " + sender.getName(), NotificationType.CREDIT_NOTIFICATION);
            notificationService.saveNotification(sender.getId(), "You sent ₦" + amount + " to " + receiver.getName(), NotificationType.DEBIT_NOTIFICATION);
            transactionService.saveTransaction(amount, TransactionType.DEBIT, sender, receiver);
            transactionService.saveTransaction(amount, TransactionType.CREDIT, sender, receiver);
         }else{
            String respond = "Reward Unsuccessful";
            return new ApiResponse<>("Insufficient balance in your Wallet", LocalDateTime.now(), respond);
        }
        String message = "A Reward of ₦" + amount +  " was sent Successfully to " + receiver.getName();
        return new ApiResponse<>("success",  LocalDateTime.now(), message);
    }
}
