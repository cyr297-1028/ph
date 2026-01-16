<template>
    <div class="container-main">
        <el-row :gutter="20">
            <el-col :span="14">
                <div class="dashboard-card">
                    <div style="margin-bottom: 20px;">
                        <PieChart fontColor="rgb(51,51,51)" tag="信息占比" :values="pieValues" :types="pieTypes" />
                    </div>
                    <div style="display: flex; justify-content: space-between;">
                        <div style="width: 49%; overflow: hidden;"> <LineChart height="250px" tag="存量用户" @on-selected="userDatesSelected" :values="userValues"
                                :date="userDates" />
                        </div>
                        <div style="width: 49%; overflow: hidden;"> <LineChart height="250px" tag="健康指标" @on-selected="modelDatesSelected" :values="modelValues"
                                :date="modelDates" />
                        </div>
                    </div>
                </div>
            </el-col>

            <el-col :span="8">
                <div class="dashboard-card message-card">
                    <div class="card-header">
                        <span class="header-title">最新消息</span>
                        <el-tag size="mini" type="primary" effect="plain">{{ messageList.length }}</el-tag>
                    </div>
                    <div class="timeline-container">
                        <div v-for="(message, index) in messageList" :key="index" class="timeline-item">
                            <div class="message-header">
                                <span class="receiver-name">{{ message.receiverName }}</span>
                                <span class="create-time">{{ time(message.createTime) }}</span>
                            </div>
                            <div class="message-content" :title="message.content">
                                {{ parseText(message.content) }}
                            </div>
                        </div>
                        <el-empty v-if="messageList.length === 0" description="暂无消息" :image-size="80"></el-empty>
                    </div>
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script>
import LineChart from "@/components/LineChart"
import PieChart from "@/components/PieChart"
import { timeAgo } from "@/utils/data"

export default {
    components: { LineChart, PieChart },
    data() {
        return {
            userValues: [],
            userDates: [],
            modelDates: [],
            modelValues: [],
            pieValues: [],
            pieTypes: [],
            messageList: []
        }
    },
    created() {
        this.userDatesSelected(365);
        this.modelDatesSelected(365);
        this.loadPieCharts();
        this.loadMessages();
    },
    methods: {
        parseText(text) {
            const pattern = /^([^;]+;){2}[^;]+$/;
            if (pattern.test(text)) {
                const parts = text.split(';');
                return parts[2];
            }
            return text;
        },
        time(createTime) {
            return timeAgo(createTime);
        },
        loadMessages() {
            const messageQueryDto = { current: 1, size: 6 }
            this.$axios.post(`/message/query`, messageQueryDto).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.messageList = data.data;
                }
            })
        },
        loadPieCharts() {
            this.$axios.get(`/views/staticControls`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.pieValues = data.data.map(entity => entity.count);
                    this.pieTypes = data.data.map(entity => entity.name);
                }
            })
        },
        modelDatesSelected(time) {
            this.$axios.get(`/user-health/daysQuery/${time}`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.modelValues = data.data.map(entity => entity.count);
                    this.modelDates = data.data.map(entity => entity.name);
                }
            })
        },
        userDatesSelected(time) {
            this.$axios.get(`/user/daysQuery/${time}`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.userValues = data.data.map(entity => entity.count);
                    this.userDates = data.data.map(entity => entity.name);
                }
            })
        },
    },
};
</script>

<style scoped lang="scss">
.container-main {
    /* 核心修改：限制宽度，防止挤压左侧菜单 */
    width: 98%; 
    margin: 10px auto; 
    padding-bottom: 20px;
}

.dashboard-card {
    background-color: #FFFFFF;
    border-radius: 8px; /* 圆角稍微加大一点 */
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    min-height: 580px;
    height: 100%;
    box-sizing: border-box;
    /* 增加过渡效果，窗口变化时更平滑 */
    transition: all 0.3s;
}

.message-card {
    display: flex;
    flex-direction: column;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0f0f0;

    .header-title {
        font-size: 16px;
        font-weight: bold;
        color: #303133;
        border-left: 4px solid #4a8bfc;
        padding-left: 10px;
    }
}

.timeline-container {
    flex: 1;
    overflow-y: auto;
    padding-right: 5px;
    
    &::-webkit-scrollbar {
        width: 4px;
    }
    &::-webkit-scrollbar-thumb {
        background: #e0e0e0;
        border-radius: 2px;
    }
}

.timeline-item {
    position: relative;
    padding-left: 20px;
    padding-bottom: 25px;
    border-left: 2px solid #f0f0f0;
    margin-left: 5px;

    &:last-child {
        border-left: 2px solid transparent;
    }

    &::before {
        content: '';
        position: absolute;
        left: -6px;
        top: 0;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: #4a8bfc;
        border: 2px solid #fff;
        box-shadow: 0 0 0 1px #4a8bfc;
    }
}

.message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 5px;

    .receiver-name {
        font-size: 14px;
        font-weight: 600;
        color: #333;
    }

    .create-time {
        font-size: 12px;
        color: #999;
    }
}

.message-content {
    font-size: 13px;
    color: #666;
    background-color: #f8fcfb;
    padding: 10px;
    border-radius: 4px;
    line-height: 1.5;
    word-break: break-all;
}
</style>