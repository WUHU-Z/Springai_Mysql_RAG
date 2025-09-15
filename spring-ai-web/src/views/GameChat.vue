<template>
  <div class="game-chat" :class="{ 'dark': isDark }">
    <div class="game-container">
      <!-- 游戏开始界面 -->
      <div v-if="!isGameStarted" class="game-start">
        <h2>蟑螂大冒险</h2>
        <div class="input-area">
          <textarea
            v-model="angerReason"
            placeholder="请输入蟑螂出现的原因（可选）..."
            rows="3"
          ></textarea>
          <button class="start-button" @click="startGame">
            开始游戏
          </button>
        </div>
      </div>

      <!-- 聊天界面 -->
      <div v-else class="chat-main">
        <!-- 游戏统计信息 -->
        <div class="game-stats">
          <div class="stat-item">
            <span class="label">
              <BugAntIcon class="bug-ant-icon" :class="{ 'beating': forgiveness >= 100 }" />
              生命值
            </span>
            <div class="progress-bar">
              <div 
                class="progress" 
                :style="{ width: `${forgiveness}%` }"
                :class="{ 
                  'low': forgiveness < 30,
                  'medium': forgiveness >= 30 && forgiveness < 70,
                  'high': forgiveness >= 70 
                }"
              ></div>
            </div>
            <span class="value">{{ forgiveness }}%</span>
          </div>
          <div class="stat-item">
            <span class="label">对话轮次</span>
            <span class="value">{{ currentRound }}/{{ MAX_ROUNDS }}</span>
          </div>
        </div>

        <div class="messages" ref="messagesRef">
          <ChatMessage
            v-for="(message, index) in currentMessages"
            :key="index"
            :message="message"
            :is-stream="isStreaming && index === currentMessages.length - 1"
          />
        </div>
        
        <div class="input-area">
          <textarea
            v-model="userInput"
            @keydown.enter.prevent="sendMessage()"
            placeholder="输入消息..."
            rows="1"
            ref="inputRef"
            :disabled="isGameOver"
          ></textarea>
          <button 
            class="send-button" 
            @click="sendMessage()"
            :disabled="isStreaming || !userInput.trim() || isGameOver"
          >
            <PaperAirplaneIcon class="icon" />
          </button>
        </div>
      </div>

      <!-- 游戏结束提示 -->
      <div v-if="isGameOver" class="game-over" :class="{ 'success': forgiveness >= 100 }">
        <div class="result">{{ gameResult }}</div>
        <button class="restart-button" @click="resetGame">
          重新开始
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useDark } from '@vueuse/core'
import { PaperAirplaneIcon, HeartIcon, BugAntIcon } from '@heroicons/vue/24/outline'
import ChatMessage from '../components/ChatMessage.vue'
import { chatAPI } from '../services/api'

const isDark = useDark()
const messagesRef = ref(null)
const inputRef = ref(null)
const userInput = ref('')
const isStreaming = ref(false)
const currentChatId = ref(null)
const currentMessages = ref([])
const angerReason = ref('')
const isGameStarted = ref(false)
const isGameOver = ref(false)
const gameResult = ref('')
const MAX_ROUNDS = 100  // 添加最大轮次常量
const currentRound = ref(0)  // 添加当前轮次计数
const forgiveness = ref(100)

// 自动调整输入框高度
const adjustTextareaHeight = () => {
  const textarea = inputRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = textarea.scrollHeight + 'px'
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

// 开始游戏
const startGame = async () => {
  isGameStarted.value = true
  isGameOver.value = false
  gameResult.value = ''
  currentChatId.value = Date.now().toString()
  currentMessages.value = []
  currentRound.value = 0
  forgiveness.value = 100  // 重置生命值
  
  // 发送开始游戏请求
  const startPrompt = angerReason.value 
    ? `开始游戏，蟑螂出现原因：${angerReason.value}`
    : '开始游戏'
  
  await sendMessage(startPrompt)
}

// 重置游戏
const resetGame = () => {
  isGameStarted.value = false
  isGameOver.value = false
  gameResult.value = ''
  currentMessages.value = []
  angerReason.value = ''
  userInput.value = ''
  currentRound.value = 0
  forgiveness.value = 100
}

// 发送消息
const sendMessage = async (content) => {
  if (isStreaming.value || (!content && !userInput.value.trim())) return
  
  // 使用传入的 content 或用户输入框的内容
  const messageContent = content || userInput.value.trim()
  
  // 添加用户消息
  const userMessage = {
    role: 'user',
    content: messageContent,
    timestamp: new Date()
  }
  currentMessages.value.push(userMessage)
  
  // 清空输入并增加轮次计数
  if (!content) {  // 只有在非传入内容时才清空输入框和计数
    userInput.value = ''
    adjustTextareaHeight()
    currentRound.value++  // 增加轮次计数
  }
  await scrollToBottom()
  
  // 添加助手消息占位
  const assistantMessage = {
    role: 'assistant',
    content: '',
    timestamp: new Date()
  }
  currentMessages.value.push(assistantMessage)
  isStreaming.value = true
  
  let accumulatedContent = ''
  
  try {
    // 确保使用正确的消息内容发送请求
    const reader = await chatAPI.sendGameMessage(messageContent, currentChatId.value)
    const decoder = new TextDecoder('utf-8')
    
    while (true) {
      try {
        const { value, done } = await reader.read()
        if (done) break
        
        // 累积新内容
        accumulatedContent += decoder.decode(value)
        
        // 尝试从回复中提取生命值
        const forgivenessMatch = accumulatedContent.match(/\s*生命值\s*(\(HP\))?[:：]\s*(\d+)\/\d+/i)
        if (forgivenessMatch) {
          const newForgiveness = parseInt(forgivenessMatch[2])
          if (!isNaN(newForgiveness)) {
            forgiveness.value = Math.min(100, Math.max(0, newForgiveness))

            // 移除冗余的判定，因为游戏结束现在完全依赖模型回复
            
          }
        }

        // 更新消息内容
        await nextTick(() => {
          const updatedMessage = {
            ...assistantMessage,
            content: accumulatedContent
          }
          const lastIndex = currentMessages.value.length - 1
          currentMessages.value.splice(lastIndex, 1, updatedMessage)
        })
        await scrollToBottom()
      } catch (readError) {
        console.error('读取流错误:', readError)
        break
      }
    }

    // 检查是否达到最大轮次，并等待本轮回复完成后再判断
    // 移除基于轮次和生命值的游戏结束判定，现在完全依赖模型回复
    if (accumulatedContent.includes('游戏结束')) {
      isGameOver.value = true
      gameResult.value = accumulatedContent
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    assistantMessage.content = '抱歉，发生了错误，请稍后重试。'
  } finally {
    isStreaming.value = false
    await scrollToBottom()
  }
}

// 添加计算属性显示剩余轮次
const remainingRounds = computed(() => MAX_ROUNDS - currentRound.value)

onMounted(() => {
  adjustTextareaHeight()
})
</script>

<style scoped lang="scss">
.game-chat {
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  background: var(--bg-color);
  overflow: hidden;
  z-index: 1;

  .game-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    max-width: 1200px;
    width: 100%;
    margin: 0 auto;
    padding: 1.5rem 2rem;
    position: relative;
    height: 100%;
  }

  .game-start {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2rem;
    min-height: 400px;
    padding: 2rem;
    background: var(--bg-color);
    border-radius: 1rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);

    h2 {
      font-size: 2rem;
      color: var(--text-color);
      margin: 0;
    }

    .input-area {
      width: 100%;
      max-width: 600px;
      display: flex;
      flex-direction: column;
      gap: 1rem;

      textarea {
        width: 100%;
        padding: 1rem;
        border: 1px solid rgba(0, 0, 0, 0.1);
        border-radius: 0.5rem;
        resize: none;
        font-family: inherit;
        font-size: 1rem;
        line-height: 1.5;

        &:focus {
          outline: none;
          border-color: #007CF0;
          box-shadow: 0 0 0 2px rgba(0, 124, 240, 0.1);
        }
      }

      .start-button {
        padding: 1rem 2rem;
        background: #007CF0;
        color: white;
        border: none;
        border-radius: 0.5rem;
        font-size: 1.1rem;
        cursor: pointer;
        transition: background-color 0.3s;

        &:hover {
          background: #0066cc;
        }
      }
    }
  }

  .chat-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 1rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
    overflow: hidden;

    .game-stats {
      position: sticky;
      top: 0;
      background: rgba(0, 0, 0, 0.7);
      color: white;
      padding: 1rem;
      z-index: 10;
      backdrop-filter: blur(5px);
      display: flex;
      gap: 2rem;
      justify-content: center;
      align-items: center;
      margin-bottom: 1rem;
      border-radius: 0.5rem;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;

        .label {
          display: flex;
          align-items: center;
          gap: 0.25rem;

          .bug-ant-icon {
            width: 1.25rem;
            height: 1.25rem;
            color: #6a9955; /* 使用一个更适合虫子的颜色 */

            &.beating {
              animation: heartbeat 1s infinite;
            }
          }
        }

        .value {
          font-size: 1rem;
          font-weight: 500;
        }

        .progress-bar {
          width: 150px;
          height: 8px;
          background: rgba(255, 255, 255, 0.2);
          border-radius: 4px;
          overflow: hidden;

          .progress {
            height: 100%;
            transition: width 0.3s ease;
            border-radius: 4px;

            &.low {
              background: #ff4d4f;
            }

            &.medium {
              background: #faad14;
            }

            &.high {
              background: #52c41a;
            }
          }
        }
      }
    }

    .messages {
      flex: 1;
      overflow-y: auto;
      padding: 2rem;
    }

    .input-area {
      flex-shrink: 0;
      padding: 1.5rem 2rem;
      background: rgba(255, 255, 255, 0.98);
      border-top: 1px solid rgba(0, 0, 0, 0.05);
      display: flex;
      gap: 1rem;
      align-items: flex-end;

      textarea {
        flex: 1;
        resize: none;
        border: 1px solid rgba(0, 0, 0, 0.1);
        background: white;
        border-radius: 0.75rem;
        padding: 1rem;
        color: inherit;
        font-family: inherit;
        font-size: 1rem;
        line-height: 1.5;
        max-height: 150px;

        &:focus {
          outline: none;
          border-color: #007CF0;
          box-shadow: 0 0 0 2px rgba(0, 124, 240, 0.1);
        }

        &:disabled {
          background: #f5f5f5;
          cursor: not-allowed;
        }
      }

      .send-button {
        background: #007CF0;
        color: white;
        border: none;
        border-radius: 0.5rem;
        width: 2.5rem;
        height: 2.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background-color 0.3s;

        &:hover:not(:disabled) {
          background: #0066cc;
        }

        &:disabled {
          background: #ccc;
          cursor: not-allowed;
        }

        .icon {
          width: 1.25rem;
          height: 1.25rem;
        }
      }
    }
  }

  .game-over {
    position: absolute;
    bottom: 6rem;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.8);
    color: white;
    padding: 1rem 2rem;
    border-radius: 0.5rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;

    .result {
      font-size: 1.1rem;
    }

    .restart-button {
      padding: 0.5rem 1rem;
      background: #007CF0;
      color: white;
      border: none;
      border-radius: 0.25rem;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background: #0066cc;
      }
    }

    &.success {
      background: rgba(82, 196, 26, 0.9);
      
      .restart-button {
        background: #52c41a;
        
        &:hover {
          background: #389e0d;
        }
      }
    }
  }
}

.dark {
  .game-start {
    .input-area {
      textarea {
        background: rgba(255, 255, 255, 0.05);
        border-color: rgba(255, 255, 255, 0.1);
        color: white;

        &:focus {
          border-color: #007CF0;
          box-shadow: 0 0 0 2px rgba(0, 124, 240, 0.2);
        }
      }
    }
  }

  .chat-main {
    background: rgba(40, 40, 40, 0.95);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.2);

    .input-area {
      background: rgba(30, 30, 30, 0.98);
      border-top: 1px solid rgba(255, 255, 255, 0.05);

      textarea {
        background: rgba(50, 50, 50, 0.95);
        border-color: rgba(255, 255, 255, 0.1);
        color: white;

        &:focus {
          border-color: #007CF0;
          box-shadow: 0 0 0 2px rgba(0, 124, 240, 0.2);
        }

        &:disabled {
          background: rgba(30, 30, 30, 0.95);
        }
      }
    }

    .game-stats {
      background: rgba(0, 0, 0, 0.8);
    }
  }
}

@keyframes heartbeat {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}
</style> 