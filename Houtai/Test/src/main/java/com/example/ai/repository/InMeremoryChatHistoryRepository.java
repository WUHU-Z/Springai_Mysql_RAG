package com.example.ai.repository;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMeremoryChatHistoryRepository implements ChatHistoryRepository{

    //初步先用内存存储
    private final Map<String,List<String>> chatHistory = new HashMap<>();
    @Override
    public void save(String type, String chatID) {
        List<String> chatIds = chatHistory.computeIfAbsent(type, k -> new ArrayList<>());
        if(chatIds.contains(chatID)){
            System.out.println("666,开了是吧");
            return;
        }
        chatIds.add(chatID);
    }

    @Override
    public List<String> getChatIDS(String type) {
        return chatHistory.getOrDefault(type, List.of());
    }
}
