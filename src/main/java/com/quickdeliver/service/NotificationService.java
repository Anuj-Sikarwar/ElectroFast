package com.quickdeliver.service;

import com.quickdeliver.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    private final WebClient.Builder webClientBuilder;

    public void sendOrderNotification(Order order) {
        try {
            String message = buildOrderMessage(order);
            sendTelegramMessage(message);
        } catch (Exception e) {
            // Never crash the order flow if notification fails
            log.error("Failed to send Telegram notification for order {}: {}",
                    order.getId(), e.getMessage());
        }
    }

    private void sendTelegramMessage(String text) {
        WebClient client = webClientBuilder
                .baseUrl("https://api.telegram.org")
                .build();

        client.post()
                .uri("/bot" + botToken + "/sendMessage")
                .bodyValue(java.util.Map.of(
                        "chat_id", chatId,
                        "text",    text,
                        "parse_mode", "HTML"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.info("Telegram notification sent for order {}", text),
                        error    -> log.error("Telegram send failed: {}", error.getMessage())
                );
    }

    private String buildOrderMessage(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 <b>New Order Received!</b>\n\n");
        sb.append("📦 <b>Order #").append(order.getId()).append("</b>\n");
        sb.append("💰 Amount: ₹").append(
                String.format("%,.0f", order.getTotalAmount())).append("\n");
        sb.append("💳 Payment: ").append(order.getPaymentMethod()).append("\n");
        sb.append("📍 Deliver to: ").append(order.getDeliveryAddress()).append("\n");
        sb.append("📞 Customer: ").append(order.getDeliveryPhone()).append("\n\n");

        sb.append("🛍️ <b>Items:</b>\n");
        if (order.getItems() != null) {
            for (var item : order.getItems()) {
                sb.append("• ").append(item.getProduct().getName())
                        .append(" x").append(item.getQuantity())
                        .append(" — ₹").append(item.getSubtotal())
                        .append("\n");
            }
        }

        sb.append("\n🕐 ").append(order.getCreatedAt().toString().substring(0, 16).replace("T", " "));
        return sb.toString();
    }
}