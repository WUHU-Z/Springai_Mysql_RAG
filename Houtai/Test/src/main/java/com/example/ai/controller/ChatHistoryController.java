package com.example.ai.controller;

import com.example.ai.entity.vo.MessageVO;
import com.example.ai.repository.ChatHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatMemory chatMemory;

    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable("type") String type) {
        return chatHistoryRepository.getChatIDS(type);
    }

    @GetMapping("/{type}/{chatID}")
    public List<MessageVO> getChatMessages(@PathVariable("type") String type, @PathVariable("chatID") String chatID) {
        var messages = chatMemory.get(chatID);
        if(messages == null) {
            return List.of();
        }
        return messages.stream().map(MessageVO::new).toList();
    }
}
