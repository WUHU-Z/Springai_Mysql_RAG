package com.example.ai.repository;

import java.util.List;
import java.util.Map;

public interface ChatHistoryRepository {
    /**
     * 保存会话记录
     * @param type 业务类型，如：chat,service,pdf
     * @param chatID 会话ID
     */

    void save(String type,String chatID);

    /**
     * 获取会话的ID列表
     * @param type  业务类型，如：chat,service,pdf
     * @return
     */
    List<String> getChatIDS(String type);
}
